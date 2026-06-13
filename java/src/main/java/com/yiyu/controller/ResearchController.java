package com.yiyu.controller;

import com.yiyu.service.DeepSeekService;
import com.yiyu.service.DocParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * 生成调研报告大纲
     */
    @PostMapping("/generate-outline")
    public Map<String, Object> generateOutline(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "");
        @SuppressWarnings("unchecked")
        List<String> fileIds = (List<String>) body.getOrDefault("fileIds", List.of());

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

            String ref = refContent.length() > 0
                    ? "\n\n参考文件内容：\n" + refContent + "\n请基于以上参考文献生成报告大纲。"
                    : "";

            String prompt = """
                    请为以下研究方向生成一份详细的调研报告大纲：%s%s

                    要求：
                    - 包含中文和英文文献的检索思路
                    - 结构完整的报告框架（背景、现状、机制、临床数据、未来方向）
                    - 每个章节注明需要涵盖的关键内容

                    请以JSON格式返回（纯JSON，不要markdown代码块）：
                    {
                      "title": "报告标题",
                      "sections": [
                        {
                          "title": "章节标题",
                          "content": "章节描述",
                          "subPoints": ["要点1", "要点2"]
                        }
                      ]
                    }
                    """.formatted(topic, ref);

            String raw = deepSeek.chatRaw("你是一个医学课题调研专家。只返回JSON，不要额外文字。", prompt);
            String outline = extractJson(raw);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("outline", outline);
            return result;

        } catch (Exception e) {
            log.error("Research outline failed", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
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
                    """.formatted(outlineJson);

            String reportText = deepSeek.chat(
                    "你是一个医学课题调研专家。生成详细、专业的调研报告文字。",
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
            log.error("Report generation failed", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 下载报告
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> download(@PathVariable String fileName) {
        try {
            File file = new File(System.getProperty("user.home") + "/yiyu/output", fileName);
            if (!file.exists()) return ResponseEntity.notFound().build();
            byte[] content = Files.readAllBytes(file.toPath());
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
        content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
        mapper.readTree(content); // validate
        return content;
    }
}
