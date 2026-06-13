package com.yiyu.controller;

import com.yiyu.service.DeepSeekService;
import com.yiyu.service.DocParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/docqa")
public class DocQAController {

    private static final Logger log = LoggerFactory.getLogger(DocQAController.class);
    private final DeepSeekService deepSeek;
    private final DocParserService docParser;

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
                text = text.substring(0, 50000) + "\n\n[内容已截断，仅显示前50000字符]";
            }

            // 初始化会话上下文
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                    "你是一个文档阅读助手。用户上传了一份文件：\n" + fileName + "\n\n文件内容如下：\n" + text +
                    "\n\n请基于以上文件内容回答用户的问题。如果问题无法从文件中找到答案，请如实告知。"));
            sessions.put(sessionId, messages);

            String summary = deepSeek.chat(
                    "你是一个文档摘要助手。用3-5句话简洁概括文件内容，不要多余文字。",
                    "请概括以下文件内容：\n" + text.substring(0, Math.min(text.length(), 3000))
            );

            return Map.of("success", true, "fileName", fileName, "summary", summary);

        } catch (Exception e) {
            log.error("Load file failed", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 基于文件内容提问
     */
    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.getOrDefault("sessionId", "default");
        String question = (String) body.get("question");

        List<Map<String, String>> messages = sessions.get(sessionId);
        if (messages == null) {
            return Map.of("success", false, "error", "请先加载文件");
        }

        try {
            // 追加用户问题到上下文
            messages.add(Map.of("role", "user", "content", question));

            // 构建完整的对话请求
            StringBuilder chatBody = new StringBuilder();
            chatBody.append("{\"model\":\"").append(deepSeek.getModel()).append("\",\"messages\":[");
            for (int i = 0; i < messages.size(); i++) {
                Map<String, String> msg = messages.get(i);
                if (i > 0) chatBody.append(",");
                chatBody.append("{\"role\":\"").append(msg.get("role"))
                        .append("\",\"content\":\"").append(escape(msg.get("content"))).append("\"}");
            }
            chatBody.append("]}");

            // 使用 HttpClient 调用
            var client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(30)).build();
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(deepSeek.getBaseUrl() + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepSeek.getApiKey())
                    .timeout(java.time.Duration.ofSeconds(120))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(chatBody.toString()))
                    .build();

            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String raw = response.body();

            // 解析回复
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(raw);
            String reply = root.get("choices").get(0).get("message").get("content").asText();

            // 追加 AI 回复到上下文
            messages.add(Map.of("role", "assistant", "content", reply));

            return Map.of("success", true, "reply", reply);

        } catch (Exception e) {
            log.error("Ask failed", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
