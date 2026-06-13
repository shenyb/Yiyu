package com.yiyu.model;

/** PPT 确认生成请求 */
public class PptConfirmRequest {
    private PptOutline outline;
    private String adjustments;  // 用户调整意见（可选）

    public PptOutline getOutline() { return outline; }
    public void setOutline(PptOutline outline) { this.outline = outline; }
    public String getAdjustments() { return adjustments; }
    public void setAdjustments(String adjustments) { this.adjustments = adjustments; }
}
