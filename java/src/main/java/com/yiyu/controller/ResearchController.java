package com.yiyu.controller;

import com.yiyu.service.DeepSeekService;
import com.yiyu.service.DocParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private static final Logger log = LoggerFactory.getLogger(ResearchController.class);
    private final DeepSeekService deepSeek;
    private final DocParserService docParser;

    public ResearchController(DeepSeekService deepSeek, DocParserService docParser) {
        this.deepSeek = deepSeek;
        this.docParser = docParser;
    }

    /**
     * P1-04: 上传参考文件，返回文件内容文本
     */
    @PostMapping("/upload-ref")
    public Map<String, Object> uploadRef(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "error", "未选择文件");
        }
        try {
            File tempFile = File.createTempFile("yiyu-research-ref-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);
            String text = docParser.extractText(tempFile);
            tempFile.delete();
            return Map.of("success", true, "fileName", file.getOriginalFilename(), "content", text);
        } catch (Exception e) {
            log.error("Research upload-ref failed", e);
            return Map.of("success", false, "error", String.valueOf(e.getMessage()));
        }
    }

    /**
     * 生成调研报告大纲
     */
    @PostMapping("/generate-outline")
    public Map<String, Object> generateOutline(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "");
        @SuppressWarnings("unchecked")
        List<String> fileIds = (List<String>) body.getOrDefault("fileIds", List.of());
        // P1-04: 前端上传文件后直接传 reference 文本
        String referenceText = (String) body.getOrDefault("reference", "");

        try {
            // 读取参考文件
            StringBuilder refContent = new StringBuilder();
            for (String fileId : fileIds) {
                File file = findUploadedFile(fileId);
                if (file != null) {
                    String text = docParser.extractText(file);
                    refContent.append("\n--- 参考文件：").append(file.getName()).append(" ---\n").append(text).append("\n");
                }
            }
            // P1-04: 前端通过 upload-ref 接口上传后直接传来的 reference 文本
            if (referenceText != null && !referenceText.isBlank()) {
                refContent.append("\n--- 参考文件内容 ---\n").append(referenceText).append("\n");
            }

            String ref = refContent.length() > 0
                    ? "\n\n参考文件内容：\n" + refContent + "\n请基于以上参考文献生成报告大纲。"
                    : "";

            String prompt = """
                    请为以下研究方向生成一份详细的调研报告大纲：%s%s

                    要求：
                    - 包含中文和英文文献的检索思路
                    - 结构完整的报告框架（背景、现状、机制、临床数据、未来方向）
                    - 每个章节注明需要涵盖的关键内容
                    - 【重要】每个要点必须注明文献来源，格式为 [出处: 作者/年份/期刊]，如无法确定则标注 [出处: 待核实]

                    请以JSON格式返回（纯JSON，不要markdown代码块）：
                    {
                      "title": "报告标题",
                      "sections": [
                        {
                          "title": "章节标题",
                          "content": "章节描述",
                          "subPoints": ["要点1 [出处: 作者/年份/期刊]", "要点2 [出处: 作者/年份/期刊]"],
                          "reference": "本章节主要参考文献"
                        }
                      ]
                    }
                    """.formatted(topic, ref);

            String raw = deepSeek.chatRaw("你是一个医学课题调研专家。只返回JSON，不要额外文字。医学内容必须标注出处，不得编造文献。", prompt);
            String outline = extractJson(raw);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("outline", outline);
            return result;

        } catch (Exception e) {
            log.error("Research outline failed, topic={}, referenceLen={}", topic,
                    referenceText == null ? 0 : referenceText.length(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            return result;
        }
    }

    /**
     * 根据大纲生成调研报告 DOCX
     */
    @PostMapping("/generate-report")
    public Map<String, Object> generateReport(@RequestBody Map<String, Object> body) {
        String outlineJson = (String) body.get("outline");
        String outputDir = System.getProperty("user.home") + "/yiyu/output";

        try {
            // 让 AI 根据大纲生成完整报告文本
            String prompt = """
                    根据以下大纲，生成一份完整的课题调研报告（中文），要求内容详实、专业：

                    %s

                    请生成完整的报告文字内容，包含每个章节的详细论述。
                    【重要】所有医学观点和数据必须标注出处，格式为[出处: 作者/年份/期刊]。
                    如无法确定具体出处，请标注[出处: 待核实]，提醒读者需要核实。
                    """.formatted(outlineJson);

            String reportText = deepSeek.chat(
                    "你是一个医学课题调研专家。生成详细、专业的调研报告文字。医学内容必须标注出处，不得编造文献。",
                    prompt
            );

            // 生成 DOCX 文件
            @SuppressWarnings("unchecked")
            Map<String, Object> outline = new com.fasterxml.jackson.databind.ObjectMapper().readValue(outlineJson, Map.class);
            String title = (String) outline.getOrDefault("title", "课题调研报告");
            String fileName = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".docx";

            File outDir = new File(outputDir);
            if (!outDir.exists()) outDir.mkdirs();
            File output = new File(outDir, fileName);

            generateDocx(output, title, reportText);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("filePath", output.getAbsolutePath());
            result.put("fileName", output.getName());
            result.put("fileSize", output.length());
            return result;

        } catch (Exception e) {
            log.error("Report generation failed, outlineLen={}", outlineJson == null ? 0 : outlineJson.length(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            return result;
        }
    }

    /**
     * 下载报告
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String file) {
        try {
            String fileName = file;
            File localFile = new File(System.getProperty("user.home") + "/yiyu/output", fileName);
            if (!localFile.exists()) return ResponseEntity.notFound().build();
            byte[] content = Files.readAllBytes(localFile.toPath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void generateDocx(File output, String title, String content) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             FileOutputStream out = new FileOutputStream(output)) {

            // 标题
            org.apache.poi.xwpf.usermodel.XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            // 正文
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;

                org.apache.poi.xwpf.usermodel.XWPFParagraph para = doc.createParagraph();
                org.apache.poi.xwpf.usermodel.XWPFRun run = para.createRun();
                run.setText(line);
                run.setFontSize(11);

                if (line.matches("^[一二三四五六七八九十]+[、.．].*")) {
                    run.setBold(true);
                    run.setFontSize(14);
                }
            }

            doc.write(out);
        }
    }

    private File findUploadedFile(String fileId) {
        File uploadDir = new File(System.getProperty("user.home") + "/yiyu/uploads");
        File[] files = uploadDir.listFiles((d, name) -> name.startsWith(fileId));
        return (files != null && files.length > 0) ? files[0] : null;
    }

    private String extractJson(String raw) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var root = mapper.readTree(raw);
        String content = root.get("choices").get(0).get("message").get("content").asText();
        log.info("extractJson: AI content length={}, first 300 = [{}], last 300 = [{}]",
                content.length(),
                content.substring(0, Math.min(content.length(), 300)),
                content.length() > 300 ? content.substring(content.length() - 300) : content);

        content = com.yiyu.service.PptService.cleanAiJson(content);

        // 第一次验证
        try {
            mapper.readTree(content);
            return content;
        } catch (Exception firstErr) {
            log.warn("extractJson: first parse failed, attempting bracket fix. Error: {}", firstErr.getMessage());
        }

        // 兜底：括号平衡修复后重试
        try {
            String fixed = com.yiyu.service.PptService.fixBracketBalance(content);
            mapper.readTree(fixed);
            log.info("extractJson: bracket fix succeeded");
            return fixed;
        } catch (Exception secondErr) {
            log.error("extractJson: JSON 修复失败, length={}, first 300 = [{}], last 300 = [{}]",
                    content.length(),
                    content.substring(0, Math.min(content.length(), 300)),
                    content.length() > 300 ? content.substring(content.length() - 300) : content, secondErr);
            throw secondErr;
        }
    }
}
