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
     * 根据主题生成 PPT 大纲 (JSON)
     */
    public String generateOutline(String topic, String referenceContent) throws Exception {
        String ref = (referenceContent != null && !referenceContent.isBlank())
                ? "\n\n参考文档内容：\n" + referenceContent + "\n\n请基于以上参考内容生成大纲。"
                : "";

        String prompt = """
                请为一个医学相关PPT生成详细大纲，主题是：%s%s

                返回严格的JSON格式（不要markdown代码块，纯JSON），结构如下：
                {
                  "title": "PPT主标题",
                  "subtitle": "副标题",
                  "slides": [
                    {
                      "title": "幻灯片标题",
                      "content": "段落内容描述",
                      "items": ["要点1", "要点2", "要点3"]
                    }
                  ]
                }

                要求：
                - 8-12张幻灯片
                - 每张幻灯片3-5个要点
                - 内容专业、准确
                - 适合15-20分钟汇报
                """.formatted(topic, ref);

        String raw = deepSeek.chatRaw(
                "你是一个医学PPT制作助手。你只返回JSON，不要加任何额外文字。",
                prompt
        );

        return extractJsonFromResponse(raw);
    }

    /**
     * 根据确认的大纲生成 PPT 文件
     */
    public File generatePpt(String outlineJson, String templatePath, String outputDir) throws Exception {
        JsonNode outline = mapper.readTree(outlineJson);

        // 确保输出目录存在
        File outDir = new File(outputDir);
        if (!outDir.exists()) outDir.mkdirs();

        String title = outline.get("title").asText("PPT");
        String fileName = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".pptx";
        File output = new File(outDir, fileName);

        // 使用模板生成 PPT
        if (templatePath != null && !templatePath.isBlank() && new File(templatePath).exists()) {
            generateFromTemplate(outline, templatePath, output);
        } else {
            // 没有模板时，用 POI 直接创建
            generateFromScratch(outline, output);
        }

        log.info("PPT generated: {}", output.getAbsolutePath());
        return output;
    }

    private void generateFromTemplate(JsonNode outline, String templatePath, File output) throws Exception {
        // POI-TL 模板填充
        com.deepoove.poi.XWPFTemplate template = com.deepoove.poi.XWPFTemplate.compile(templatePath)
                .render(convertOutlineToMap(outline));
        try (FileOutputStream out = new FileOutputStream(output)) {
            template.write(out);
        }
        template.close();
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
     * 从 AI 回复中提取 JSON（去掉 markdown 代码块等干扰）
     */
    private String extractJsonFromResponse(String raw) throws Exception {
        // 从完整的 OpenAI 响应中提取 content
        JsonNode root = mapper.readTree(raw);
        String content = root.get("choices").get(0).get("message").get("content").asText();

        // 去掉可能的 markdown 代码块
        content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

        // 验证是合法 JSON
        mapper.readTree(content);
        return content;
    }
}
