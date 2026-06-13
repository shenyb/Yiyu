package com.yiyu.controller;

import com.yiyu.service.DeepSeekService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DeepSeekService deepSeek;

    public ChatController(DeepSeekService deepSeek) {
        this.deepSeek = deepSeek;
    }

    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody Map<String, Object> body) {
        String message = (String) body.getOrDefault("message", "");
        try {
            String reply = deepSeek.chat(
                    "你是医语，一个帮助医生做PPT、课题调研和读文档的AI助手。回答简洁、专业、友善。",
                    message
            );
            return Map.of("reply", reply);
        } catch (Exception e) {
            return Map.of("reply", "没成功，换个说法试试？😅\n\n" + e.getMessage());
        }
    }
}
