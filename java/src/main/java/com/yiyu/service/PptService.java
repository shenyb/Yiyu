package com.yiyu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

@Service
public class PptService {

    private static final Logger log = LoggerFactory.getLogger(PptService.class);
    private final DeepSeekService deepSeek;
    private final ObjectMapper mapper = new ObjectMapper();

    public PptService(DeepSeekService deepSeek) {
        this.deepSeek = deepSeek;
    }

    /**
     * 根据主题生成 PPT 大纲 (JSON)，支持基于上版大纲修改
     */
    public String generateOutline(String topic, String referenceContent, String prevOutline) throws Exception {
        log.info(">>> generateOutline() topic={}, refLen={}, hasPrev={}", topic,
                referenceContent != null ? referenceContent.length() : 0,
                prevOutline != null && !prevOutline.isBlank());
        String ref = (referenceContent != null && !referenceContent.isBlank())
                ? "\n\n参考文档内容：\n" + referenceContent + "\n\n请基于以上参考内容生成大纲。"
                : "";

        String modifyHint = "";
        if (prevOutline != null && !prevOutline.isBlank()) {
            modifyHint = "\n\n以下是之前生成的大纲，请根据用户的修改意见调整：\n" + prevOutline
                    + "\n\n请在以上大纲基础上修改，保持JSON结构不变。";
        }

        String prompt = """
                请为一个医学相关PPT生成详细大纲，主题是：%s%s%s

                返回严格的JSON格式（不要markdown代码块，纯JSON），结构如下：
                {
                  "title": "PPT主标题",
                  "subtitle": "副标题",
                  "slides": [
                    {
                      "title": "幻灯片标题",
                      "content": "段落内容描述",
                      "items": ["要点1 [出处: 作者/年份/期刊]", "要点2 [出处: 作者/年份/期刊]", "要点3 [出处: 作者/年份/期刊]"],
                      "reference": "本页主要参考文献"
                    }
                  ]
                }

                要求：
                - 8-12张幻灯片
                - 每张幻灯片3-5个要点
                - 内容专业、准确
                - 适合15-20分钟汇报
                - 【重要】每个要点必须附带 [出处: 作者/年份/期刊] 格式的引用，如无法确定具体出处则标注 [出处: 待核实]
                - 每张幻灯片的 reference 字段列出本页主要参考文献
                """.formatted(topic, ref, modifyHint);

        String raw = deepSeek.chatRaw(
                "你是一个医学PPT制作助手。你只返回JSON，不要加任何额外文字。医学内容必须标注出处，不得编造文献。",
                prompt
        );

        String result = extractJsonFromResponse(raw);
        log.info("<<< generateOutline() outlineLen={}", result.length());
        return result;
    }

    /**
     * 根据确认的大纲生成 PPT 文件
     */
    public File generatePpt(String outlineJson, String templatePath, String outputDir) throws Exception {
        log.info(">>> generatePpt() templatePath={}, outputDir={}", templatePath, outputDir);
        log.debug("outlineJson={}", outlineJson.length() > 500 ? outlineJson.substring(0, 500) + "..." : outlineJson);
        JsonNode outline = mapper.readTree(outlineJson);

        // 确保输出目录存在
        File outDir = new File(outputDir);
        if (!outDir.exists()) outDir.mkdirs();

        String title = outline.get("title").asText("PPT");
        String fileName = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".pptx";
        File output = new File(outDir, fileName);

        // 使用模板生成 PPT（优先用户上传的模板，其次默认模板，最后无模板）
        File defaultTemplate = new File(System.getProperty("user.home") + "/yiyu/templates/default-template.pptx");
        String effectiveTemplate = null;
        if (templatePath != null && !templatePath.isBlank() && new File(templatePath).exists()) {
            effectiveTemplate = templatePath;
        } else if (defaultTemplate.exists()) {
            effectiveTemplate = defaultTemplate.getAbsolutePath();
        }
        if (effectiveTemplate != null) {
            log.info("Using PPT template: {}", effectiveTemplate);
            generateFromTemplate(outline, effectiveTemplate, output);
        } else {
            generateFromScratch(outline, output);
        }

        log.info("<<< generatePpt() file={}, size={}B", output.getName(), output.length());
        return output;
    }

    private void generateFromTemplate(JsonNode outline, String templatePath, File output) throws Exception {
        log.info("generateFromTemplate: using template {}", templatePath);
        // 直接基于模板文件创建新 PPT，保留母版/布局/主题/背景
        org.apache.poi.xslf.usermodel.XMLSlideShow ppt =
                new org.apache.poi.xslf.usermodel.XMLSlideShow(new java.io.FileInputStream(templatePath));

        // 删除模板中所有已有的幻灯片，只保留母版/布局
        while (!ppt.getSlides().isEmpty()) {
            ppt.removeSlide(0);
        }

        String title = outline.get("title").asText("PPT");
        String subtitle = outline.has("subtitle") ? outline.get("subtitle").asText("") : "";
        JsonNode slides = outline.get("slides");

        // 获取模板的布局
        org.apache.poi.xslf.usermodel.XSLFSlideLayout layout = null;
        java.util.List<org.apache.poi.xslf.usermodel.XSLFSlideMaster> masters = ppt.getSlideMasters();
        if (!masters.isEmpty()) {
            org.apache.poi.xslf.usermodel.XSLFSlideMaster master = masters.get(0);
            org.apache.poi.xslf.usermodel.XSLFSlideLayout[] layouts = master.getSlideLayouts();
            if (layouts != null && layouts.length > 0) {
                layout = layouts[0];
            }
        }

        // 标题页
        org.apache.poi.xslf.usermodel.XSLFSlide titleSlide;
        if (layout != null) {
            titleSlide = ppt.createSlide(layout);
        } else {
            titleSlide = ppt.createSlide();
        }
        org.apache.poi.xslf.usermodel.XSLFTextBox titleBox = titleSlide.createTextBox();
        titleBox.setAnchor(new java.awt.Rectangle(50, 150, 600, 80));
        org.apache.poi.xslf.usermodel.XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        org.apache.poi.xslf.usermodel.XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(title);
        titleRun.setFontSize(28.0);
        titleRun.setBold(true);
        if (!subtitle.isEmpty()) {
            org.apache.poi.xslf.usermodel.XSLFTextBox subBox = titleSlide.createTextBox();
            subBox.setAnchor(new java.awt.Rectangle(50, 240, 600, 40));
            org.apache.poi.xslf.usermodel.XSLFTextParagraph subPara = subBox.addNewTextParagraph();
            org.apache.poi.xslf.usermodel.XSLFTextRun subRun = subPara.addNewTextRun();
            subRun.setText(subtitle);
            subRun.setFontSize(16.0);
        }

        // 内容页
        if (slides != null && slides.isArray()) {
            for (JsonNode slide : slides) {
                org.apache.poi.xslf.usermodel.XSLFSlide s;
                if (layout != null) {
                    s = ppt.createSlide(layout);
                } else {
                    s = ppt.createSlide();
                }

                // 标题
                String slideTitle = slide.get("title").asText("");
                org.apache.poi.xslf.usermodel.XSLFTextBox titleTb = s.createTextBox();
                titleTb.setAnchor(new java.awt.Rectangle(50, 30, 600, 40));
                org.apache.poi.xslf.usermodel.XSLFTextParagraph stPara = titleTb.addNewTextParagraph();
                org.apache.poi.xslf.usermodel.XSLFTextRun stRun = stPara.addNewTextRun();
                stRun.setText(slideTitle);
                stRun.setFontSize(20.0);
                stRun.setBold(true);

                // 要点
                JsonNode items = slide.get("items");
                if (items != null && items.isArray()) {
                    org.apache.poi.xslf.usermodel.XSLFTextBox bodyTb = s.createTextBox();
                    bodyTb.setAnchor(new java.awt.Rectangle(50, 80, 600, 380));
                    for (JsonNode item : items) {
                        org.apache.poi.xslf.usermodel.XSLFTextParagraph para = bodyTb.addNewTextParagraph();
                        para.setBullet(true);
                        org.apache.poi.xslf.usermodel.XSLFTextRun run = para.addNewTextRun();
                        run.setText(item.asText());
                        run.setFontSize(14.0);
                    }
                }

                // 参考文献（小字）
                if (slide.has("reference") && !slide.get("reference").asText("").isEmpty()) {
                    org.apache.poi.xslf.usermodel.XSLFTextBox refTb = s.createTextBox();
                    refTb.setAnchor(new java.awt.Rectangle(50, 460, 600, 30));
                    org.apache.poi.xslf.usermodel.XSLFTextParagraph refPara = refTb.addNewTextParagraph();
                    org.apache.poi.xslf.usermodel.XSLFTextRun refRun = refPara.addNewTextRun();
                    refRun.setText("参考文献：" + slide.get("reference").asText());
                    refRun.setFontSize(9.0);
                    refRun.setFontColor(java.awt.Color.GRAY);
                }
            }
        }

        try (java.io.FileOutputStream out = new java.io.FileOutputStream(output)) {
            ppt.write(out);
        }
        ppt.close();
    }

    private void generateFromScratch(JsonNode outline, File output) throws Exception {
        // 手动用 Apache POI 创建 PPTX
        org.apache.poi.xslf.usermodel.XMLSlideShow ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow();

        // 标题幻灯片
        org.apache.poi.xslf.usermodel.XSLFSlide titleSlide = ppt.createSlide();
        org.apache.poi.xslf.usermodel.XSLFTextBox titleBox = titleSlide.createTextBox();
        titleBox.setText(outline.get("title").asText());
        titleBox.setAnchor(new java.awt.Rectangle(50, 100, 600, 100));

        // 内容幻灯片
        JsonNode slides = outline.get("slides");
        if (slides != null && slides.isArray()) {
            for (JsonNode slide : slides) {
                org.apache.poi.xslf.usermodel.XSLFSlide s = ppt.createSlide();
                org.apache.poi.xslf.usermodel.XSLFTextBox tb = s.createTextBox();
                tb.setAnchor(new java.awt.Rectangle(50, 50, 600, 400));

                StringBuilder text = new StringBuilder();
                text.append(slide.get("title").asText()).append("\n\n");
                JsonNode items = slide.get("items");
                if (items != null && items.isArray()) {
                    for (JsonNode item : items) {
                        text.append("• ").append(item.asText()).append("\n");
                    }
                }
                tb.setText(text.toString());
            }
        }

        try (FileOutputStream out = new FileOutputStream(output)) {
            ppt.write(out);
        }
        ppt.close();
    }

    private Map<String, Object> convertOutlineToMap(JsonNode outline) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", outline.get("title").asText());
        map.put("subtitle", outline.has("subtitle") ? outline.get("subtitle").asText() : "");

        List<Map<String, Object>> slides = new ArrayList<>();
        JsonNode slidesNode = outline.get("slides");
        if (slidesNode != null && slidesNode.isArray()) {
            for (JsonNode slide : slidesNode) {
                Map<String, Object> slideMap = new HashMap<>();
                slideMap.put("title", slide.get("title").asText());
                slideMap.put("content", slide.has("content") ? slide.get("content").asText() : "");

                List<String> items = new ArrayList<>();
                JsonNode itemsNode = slide.get("items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode item : itemsNode) {
                        items.add(item.asText());
                    }
                }
                slideMap.put("items", items);
                slides.add(slideMap);
            }
        }
        map.put("slides", slides);
        return map;
    }

    /**
     * 从 AI 回复中提取 JSON（去掉 markdown 代码块、尾逗号等干扰）
     */
    private String extractJsonFromResponse(String raw) throws Exception {
        // 从完整的 OpenAI 响应中提取 content
        JsonNode root = mapper.readTree(raw);
        String content = root.get("choices").get(0).get("message").get("content").asText();
        log.info("extractJsonFromResponse: AI content length={}, first 300 = [{}], last 300 = [{}]",
                content.length(),
                content.substring(0, Math.min(content.length(), 300)),
                content.length() > 300 ? content.substring(content.length() - 300) : content);

        content = cleanAiJson(content);

        // 第一次验证
        try {
            mapper.readTree(content);
            return content;
        } catch (Exception firstErr) {
            log.warn("extractJsonFromResponse: first parse failed, attempting bracket fix. Error: {}", firstErr.getMessage());
        }

        // 兜底：括号平衡修复后重试
        try {
            String fixed = fixBracketBalance(content);
            mapper.readTree(fixed); // validate
            log.info("extractJsonFromResponse: bracket fix succeeded");
            return fixed;
        } catch (Exception secondErr) {
            log.error("extractJsonFromResponse: JSON 修复失败, length={}, first 300 = [{}], last 300 = [{}]",
                    content.length(),
                    content.substring(0, Math.min(content.length(), 300)),
                    content.length() > 300 ? content.substring(content.length() - 300) : content, secondErr);
            throw secondErr;
        }
    }

    /**
     * 清理 AI 返回的 JSON：去 markdown 代码块、找 {} 边界、去尾逗号、去注释
     */
    public static String cleanAiJson(String content) {
        // 去掉可能的 markdown 代码块
        content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

        // 尝试找到 JSON 对象边界 { ... }
        int jsonStart = content.indexOf('{');
        int jsonEnd = content.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            content = content.substring(jsonStart, jsonEnd + 1).trim();
        }

        // 去掉 JS 风格注释 (// ... 和 /* ... */)
        content = content.replaceAll("//[^\n]*", "").replaceAll("/\\*.*?\\*/", "");

        // 去尾逗号：数组/对象最后一个元素后的逗号，如 [1,2,] → [1,2] 或 {"a":1,} → {"a":1}
        // 多轮清理直到不再变化
        String prev;
        do {
            prev = content;
            // ,] → ]  ,} → }  ,\s*] → ]  ,\s*} → }
            content = content.replaceAll(",\\s*]", "]").replaceAll(",\\s*}", "}");
        } while (!content.equals(prev));

        return content;
    }

    /**
     * 括号平衡修复：逐字符扫描，当遇到不匹配的 ] 或 } 时替换为正确的闭合符。
     * 例如 AI 输出 {... "items": [...} 时，把 } 替换为 ]。
     */
    public static String fixBracketBalance(String json) {
        StringBuilder sb = new StringBuilder(json.length());
        java.util.Deque<Character> stack = new java.util.ArrayDeque<>();
        boolean inStr = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // 字符串内跟踪
            if (!escape && c == '"') {
                inStr = !inStr;
            }
            if (inStr) {
                escape = !escape && c == '\\';
                sb.append(c);
                continue;
            }
            // 非字符串内
            switch (c) {
                case '{' -> { stack.push('{'); sb.append(c); }
                case '[' -> { stack.push('['); sb.append(c); }
                case '}' -> {
                    if (!stack.isEmpty() && stack.peek() == '{') {
                        stack.pop();
                        sb.append(c);
                    } else if (!stack.isEmpty() && stack.peek() == '[') {
                        // 应该是 ] 但 AI 写了 }
                        stack.pop();
                        sb.append(']');
                    } else {
                        sb.append(c); // 无匹配，原样保留
                    }
                }
                case ']' -> {
                    if (!stack.isEmpty() && stack.peek() == '[') {
                        stack.pop();
                        sb.append(c);
                    } else if (!stack.isEmpty() && stack.peek() == '{') {
                        // 应该是 } 但 AI 写了 ]
                        stack.pop();
                        sb.append('}');
                    } else {
                        sb.append(c);
                    }
                }
                default -> sb.append(c);
            }
        }
        // 补齐未闭合的括号
        while (!stack.isEmpty()) {
            char open = stack.pop();
            sb.append(open == '{' ? '}' : ']');
        }
        return sb.toString();
    }
}
