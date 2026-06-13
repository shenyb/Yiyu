package com.yiyu.model;

/** PPT 大纲中的单条目 */
public class OutlineItem {
    private String title;
    private String type;       // "title" | "content"
    private String content;    // 纯文本
    private String[] items;    // 要点列表

    public OutlineItem() {}
    public OutlineItem(String title, String type, String content, String[] items) {
        this.title = title; this.type = type; this.content = content; this.items = items;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String[] getItems() { return items; }
    public void setItems(String[] items) { this.items = items; }
}
