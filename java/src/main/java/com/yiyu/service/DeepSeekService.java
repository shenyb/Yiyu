package com.yiyu.service;

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

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

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
                .timeout(Duration.ofSeconds(60))
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
                .timeout(Duration.ofSeconds(120))
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

        log.info("<<< chatRaw() OK status=200, elapsed={}ms, rawLen={}", elapsed, response.body().length());
        return response.body();
    }

    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getModel() { return model; }

    private String buildRequestBody(String systemPrompt, String userMessage) {
        return """
            {
                "model": "%s",
                "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                ]
            }
            """.formatted(model, escape(systemPrompt), escape(userMessage));
    }

    private String parseResponse(String json) {
        log.debug("parseResponse raw={}", truncate(json, 500));
        int start = json.indexOf("\"content\":\"");
        if (start == -1) return "（AI 回复解析失败）";
        start += 11;
        int end = json.indexOf("\"", start);
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\t", "\t");
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String truncate(String s, int max) {
        return s == null ? "null" : (s.length() <= max ? s : s.substring(0, max) + "...");
    }
}
