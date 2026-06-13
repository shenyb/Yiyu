package com.yiyu.controller;

import com.yiyu.service.PptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ppt")
public class PptController {

    private static final Logger log = LoggerFactory.getLogger(PptController.class);
    private final PptService pptService;

    public PptController(PptService pptService) {
        this.pptService = pptService;
    }

    /**
     * 根据主题生成大纲
     */
    @PostMapping("/generate-outline")
    public Map<String, Object> generateOutline(@RequestBody Map<String, Object> body) {
        String topic = (String) body.getOrDefault("topic", "");
        String reference = (String) body.getOrDefault("reference", "");

        try {
            String outline = pptService.generateOutline(topic, reference);
            Map<String, Object> result = new HashMap<>();
            result.put("outline", outline);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.error("Failed to generate outline", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
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
            log.error("Failed to generate PPT", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 下载生成的 PPT 文件
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> download(@PathVariable String fileName) {
        try {
            File file = new File(System.getProperty("user.home") + "/yiyu/output", fileName);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            byte[] content = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
