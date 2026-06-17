package com.yiyu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiyu.service.DeepSeekService;
import com.yiyu.service.DocParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/docqa")
public class DocQAController {

    private static final Logger log = LoggerFactory.getLogger(DocQAController.class);
    private final DeepSeekService deepSeek;
    private final DocParserService docParser;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 会话级别的文件内容缓存（生产环境应换数据库）
    private final Map<String, List<Map<String, String>>> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    public DocQAController(DeepSeekService deepSeek, DocParserService docParser) {
        this.deepSeek = deepSeek;
        this.docParser = docParser;
    }

    /**
     * 上传并读取文件
     */
    @PostMapping("/load")
    public Map<String, Object> loadFile(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.getOrDefault("sessionId", "default");
        String filePath = (String) body.get("filePath");
        String fileContent = (String) body.get("fileContent");

        try {
            String text;
            String fileName;

            if (fileContent != null && !fileContent.isBlank()) {
                // 浏览器上传：直接使用文件内容
                text = fileContent;
                fileName = filePath != null ? filePath : "未命名文件";
            } else {
                // 桌面端：从本地文件路径读取
                File file = new File(filePath);
                if (!file.exists()) {
                    return Map.of("success", false, "error", "文件不存在：" + filePath);
                }
                text = docParser.extractText(file);
                fileName = file.getName();
            }

            // 截断过长内容
            if (text.length() > 50000) {
                text = text.substring(0, 50000) + "\n\n[文件内容过长，已截断]";
            }

            // 存入会话
            sessions.computeIfAbsent(sessionId, k -> new ArrayList<>())
                    .add(Map.of("fileName", fileName, "content", text));

            // 生成摘要
            String summary = generateSummary(text);

            return Map.of("success", true, "fileName", fileName, "summary", summary);

        } catch (Exception e) {
            log.error("Failed to load file, sessionId={}, filePath={}", sessionId, filePath, e);
            return Map.of("success", false, "error", String.valueOf(e.getMessage()));
        }
    }

    /**
     * 上传文件（multipart）— 浏览器版 PDF/DOCX 支持
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "error", "未选择文件");
        }

        String sessionId = "doc-" + System.currentTimeMillis();

        try {
            // 保存到临时文件
            File tempFile = File.createTempFile("yiyu-upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            String text = docParser.extractText(tempFile);
            String fileName = file.getOriginalFilename();

            // 截断过长内容
            if (text.length() > 50000) {
                text = text.substring(0, 50000) + "\n\n[文件内容过长，已截断]";
            }

            // 存入会话
            sessions.computeIfAbsent(sessionId, k -> new ArrayList<>())
                    .add(Map.of("fileName", fileName, "content", text));

            String summary = generateSummary(text);

            // 清理临时文件
            tempFile.delete();

            return Map.of("success", true, "sessionId", sessionId, "fileName", fileName, "summary", summary);

        } catch (Exception e) {
            log.error("Failed to upload file, originalFilename={}", file.getOriginalFilename(), e);
            return Map.of("success", false, "error", String.valueOf(e.getMessage()));
        }
    }

    /**
     * 提问
     */
    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.getOrDefault("sessionId", "default");
        String question = (String) body.getOrDefault("question", "");

        List<Map<String, String>> docs = sessions.get(sessionId);
        if (docs == null || docs.isEmpty()) {
            return Map.of("success", false, "error", "请先上传文件");
        }

        try {
            // 构建上下文
            StringBuilder context = new StringBuilder();
            for (Map<String, String> doc : docs) {
                context.append("文件：").append(doc.get("fileName")).append("\n")
                        .append(doc.get("content")).append("\n\n");
            }

            String systemPrompt = "你是一个文档问答助手。请根据以下文档内容回答用户的问题。" +
                    "如果文档中没有相关信息，请明确告知。不要编造内容。\n\n" + context;

            String reply = deepSeek.chat(systemPrompt, question);

            return Map.of("success", true, "reply", reply);

        } catch (Exception e) {
            log.error("Ask failed, sessionId={}, question={}", sessionId, question, e);
            return Map.of("success", false, "error", String.valueOf(e.getMessage()));
        }
    }

    private String generateSummary(String text) {
        try {
            String preview = text.length() > 2000 ? text.substring(0, 2000) : text;
            return deepSeek.chat(
                    "你是一个文档摘要助手。请用1-2句话概括以下文档的主要内容。",
                    preview
            );
        } catch (Exception e) {
            return text.length() > 100 ? text.substring(0, 100) + "..." : text;
        }
    }

    /**
     * P2-05: 使用 Jackson 序列化，替代手写 escape（已删除有 bug 的 escape 方法）
     * 如需 JSON 序列化字符串，直接使用 objectMapper.writeValueAsString()
     */
}
