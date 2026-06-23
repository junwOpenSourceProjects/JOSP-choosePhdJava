package josp.choosphd.api.trend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class TrendRowDTO {
    private String urlId;
    private String nameEn;
    private String sourceCode;
    private String trendType;
    private Short baseYear;
    private Short targetYear;
    private Integer rankFrom;
    private Integer rankTo;
    private Integer rankChange;

    public String getUrlId() { return urlId; }
    public void setUrlId(String v) { this.urlId = v; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String v) { this.nameEn = v; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String v) { this.sourceCode = v; }
    public String getTrendType() { return trendType; }
    public void setTrendType(String v) { this.trendType = v; }
    public Short getBaseYear() { return baseYear; }
    public void setBaseYear(Short v) { this.baseYear = v; }
    public Short getTargetYear() { return targetYear; }
    public void setTargetYear(Short v) { this.targetYear = v; }
    public Integer getRankFrom() { return rankFrom; }
    public void setRankFrom(Integer v) { this.rankFrom = v; }
    public Integer getRankTo() { return rankTo; }
    public void setRankTo(Integer v) { this.rankTo = v; }
    public Integer getRankChange() { return rankChange; }
    public void setRankChange(Integer v) { this.rankChange = v; }
}
