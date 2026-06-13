package com.yiyu.model;

import java.util.List;

/** 对话请求 */
public class ChatRequest {
    private String message;
    private List<String> fileIds;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getFileIds() { return fileIds; }
    public void setFileIds(List<String> fileIds) { this.fileIds = fileIds; }
}
