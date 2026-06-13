package com.yiyu.model;

import java.util.List;

/** AI 返回的大纲 */
public class PptOutline {
    private String pptTitle;
    private String subtitle;
    private String date;
    private List<OutlineItem> slides;

    public String getPptTitle() { return pptTitle; }
    public void setPptTitle(String pptTitle) { this.pptTitle = pptTitle; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public List<OutlineItem> getSlides() { return slides; }
    public void setSlides(List<OutlineItem> slides) { this.slides = slides; }
}
