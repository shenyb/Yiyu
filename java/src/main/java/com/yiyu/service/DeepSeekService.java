package com.yiyu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    // P1-01: 超时时间可配置，默认 180s
    @Value("${deepseek.timeout-seconds:180}")
    private int timeoutSeconds;

    public String chat(String systemPrompt, String userMessage) throws Exception {
        log.info(">>> chat() model={}, promptLen={}, msgLen={}", model,
                systemPrompt.length(), userMessage.length());
        log.debug("prompt={}", truncate(systemPrompt, 200));
        log.debug("message={}", truncate(userMessage, 200));

        String body = buildRequestBody(systemPrompt, userMessage);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        long t0 = System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long elapsed = System.currentTimeMillis() - t0;

        if (response.statusCode() != 200) {
            log.error("<<< chat() FAIL status={}, elapsed={}ms, body={}",
                    response.statusCode(), elapsed, truncate(response.body(), 300));
            throw new RuntimeException("DeepSeek API error: " + response.statusCode() + " " + response.body());
        }

        String result = parseResponse(response.body());
        log.info("<<< chat() OK status=200, elapsed={}ms, replyLen={}", elapsed, result.length());
        return result;
    }

    public String chatRaw(String systemPrompt, String userMessage) throws Exception {
        log.info(">>> chatRaw() model={}, promptLen={}, msgLen={}", model,
                systemPrompt.length(), userMessage.length());

        String body = buildRequestBody(systemPrompt, userMessage);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        long t0 = System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long elapsed = System.currentTimeMillis() - t0;

        if (response.statusCode() != 200) {
            log.error("<<< chatRaw() FAIL status={}, elapsed={}ms, body={}",
                    response.statusCode(), elapsed, truncate(response.body(), 300));
            throw new RuntimeException("DeepSeek API error: " + response.statusCode() + " " + response.body());
        }

        log.info("<<< chatRaw() OK status=200, elapsed={}ms, bodyLen={}", elapsed, response.body().length());
        return response.body();
    }

    /**
     * P1-03 + P2-05: 使用 Jackson 构建 JSON 请求体，彻底消除手写 escape 的 bug
     */
    private String buildRequestBody(String systemPrompt, String userMessage) throws Exception {
        var root = objectMapper.createObjectNode();
        root.put("model", model);
        var messages = root.putArray("messages");
        var sysMsg = messages.addObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        var userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        return objectMapper.writeValueAsString(root);
    }

    /**
     * P1-03: 使用 Jackson 解析 OpenAI 响应，替代手写 indexOf 解析
     */
    private String parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode contentNode = root.get("choices").get(0).get("message").get("content");
            if (contentNode != null) {
                return contentNode.asText();
            }
            log.warn("parseResponse: content 字段为空, raw={}", truncate(json, 300));
            return "（AI 回复解析失败：content 字段为空）";
        } catch (Exception e) {
            log.error("parseResponse: JSON 解析失败, raw={}", truncate(json, 300), e);
            return "（AI 回复解析失败）";
        }
    }

    private String truncate(String s, int max) {
        return s == null ? "null" : (s.length() <= max ? s : s.substring(0, max) + "...");
    }
}
