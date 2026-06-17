package com.yiyu.controller;

import com.yiyu.service.DocParserService;
import com.yiyu.service.PptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ppt")
public class PptController {

    private static final Logger log = LoggerFactory.getLogger(PptController.class);
    private final PptService pptService;
    private final DocParserService docParser;

    public PptController(PptService pptService, DocParserService docParser) {
        this.pptService = pptService;
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
            File tempFile = File.createTempFile("yiyu-ppt-ref-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);
            String text = docParser.extractText(tempFile);
            tempFile.delete();
            return Map.of("success", true, "fileName", file.getOriginalFilename(), "content", text);
        } catch (Exception e) {
            log.error("PPT upload-ref failed", e);
            return Map.of("success", false, "error", String.valueOf(e.getMessage()));
        }
    }

    /**
     * 上传 PPT 模板文件，保存到 ~/yiyu/templates/
     */
    @PostMapping("/upload-template")
    public Map<String, Object> uploadTemplate(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "error", "未选择模板文件");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pptx")) {
            return Map.of("success", false, "error", "模板必须是 .pptx 格式");
        }
        try {
            File tplDir = new File(System.getProperty("user.home") + "/yiyu/templates");
            if (!tplDir.exists()) tplDir.mkdirs();
            // 用时间戳避免冲突
            String savedName = System.currentTimeMillis() + "-" + originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
            File saved = new File(tplDir, savedName);
            file.transferTo(saved);
            log.info("PPT template uploaded: {} -> {}", originalName, saved.getAbsolutePath());
            return Map.of("success", true, "templatePath", saved.getAbsolutePath(), "templateName", originalName);
        } catch (Exception e) {
            log.error("PPT template upload failed", e);
            return Map.of("success", false, "error", String.valueOf(e.getMessage()));
        }
    }

    /**
     * 列出所有可用模板
     */
    @GetMapping("/templates")
    public Map<String, Object> listTemplates() {
        File tplDir = new File(System.getProperty("user.home") + "/yiyu/templates");
        List<Map<String, String>> templates = new ArrayList<>();
        if (tplDir.exists() && tplDir.isDirectory()) {
            File[] files = tplDir.listFiles((d, name) -> name.toLowerCase().endsWith(".pptx"));
            if (files != null) {
                for (File f : files) {
                    // 去掉时间戳前缀，恢复原始名
                    String name = f.getName();
                    int dashIdx = name.indexOf('-');
                    String displayName = (dashIdx > 0 && name.substring(0, dashIdx).matches("\\d+"))
                            ? name.substring(dashIdx + 1) : name;
                    templates.add(Map.of(
                            "path", f.getAbsolutePath(),
                            "name", displayName,
                            "size", String.valueOf(f.length())
                    ));
                }
            }
        }
        return Map.of("success", true, "templates", templates);
    }

    /**
     * 根据主题生成大纲
     */
    @PostMapping("/generate-outline")
    public Map<String, Object> generateOutline(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "");
        String reference = (String) body.getOrDefault("reference", "");
        String prevOutline = (String) body.getOrDefault("prevOutline", "");
        try {
            String outline = pptService.generateOutline(topic, reference, prevOutline);
            Map<String, Object> result = new HashMap<>();
            result.put("outline", outline);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.error("Failed to generate outline, topic={}, referenceLen={}", topic,
                    reference == null ? 0 : reference.length(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", String.valueOf(e.getMessage()));
            return result;
        }
    }

    /**
     * 确认大纲，生成 PPT 文件
     */
    @PostMapping("/confirm")
    public Map<String, Object> confirmPpt(@RequestBody Map<String, Object> body) {
        String outlineJson = (String) body.get("outline");
        String templatePath = (String) body.getOrDefault("templatePath", "");
        String outputDir = System.getProperty("user.home") + "/yiyu/output";

        try {
            File ppt = pptService.generatePpt(outlineJson, templatePath, outputDir);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("filePath", ppt.getAbsolutePath());
            result.put("fileName", ppt.getName());
            result.put("fileSize", ppt.length());
            return result;
        } catch (Exception e) {
            log.error("Failed to generate PPT, outlineLen={}", outlineJson == null ? 0 : outlineJson.length(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", String.valueOf(e.getMessage()));
            return result;
        }
    }

    /**
     * 下载生成的 PPT 文件
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
}