package com.choosephd.dto;

public class UniversitySourceSummary {

    private Integer sourceId;
    private String sourceNameZh;
    private String sourceNameEn;
    private Integer latestYear;
    private String latestRankDisplay;
    private Integer latestRankValue;

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceNameZh() {
        return sourceNameZh;
    }

    public void setSourceNameZh(String sourceNameZh) {
        this.sourceNameZh = sourceNameZh;
    }

    public String getSourceNameEn() {
        return sourceNameEn;
    }

    public void setSourceNameEn(String sourceNameEn) {
        this.sourceNameEn = sourceNameEn;
    }

    public Integer getLatestYear() {
        return latestYear;
    }

    public void setLatestYear(Integer latestYear) {
        this.latestYear = latestYear;
    }

    public String getLatestRankDisplay() {
        return latestRankDisplay;
    }

    public void setLatestRankDisplay(String latestRankDisplay) {
        this.latestRankDisplay = latestRankDisplay;
    }

    public Integer getLatestRankValue() {
        return latestRankValue;
    }

    public void setLatestRankValue(Integer latestRankValue) {
        this.latestRankValue = latestRankValue;
    }
}
