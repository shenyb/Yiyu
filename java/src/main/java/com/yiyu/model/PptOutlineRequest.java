package com.yiyu.model;

import java.util.List;

/** PPT 大纲生成请求 */
public class PptOutlineRequest {
    private String topic;
    private List<String> fileIds;

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public List<String> getFileIds() { return fileIds; }
    public void setFileIds(List<String> fileIds) { this.fileIds = fileIds; }
}
