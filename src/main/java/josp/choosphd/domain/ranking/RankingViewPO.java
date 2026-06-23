package josp.choosphd.domain.ranking;

/**
 * 榜单视图 PO(JOIN 后的人类可读视图,内部 Service 用)
 */
public class RankingViewPO {
    private Long id;
    private Integer rank;
    private java.math.BigDecimal score;
    private Short year;
    private String urlId;
    private String nameEn;
    private String nameZh;
    private String logoUrl;
    private String regionCode;
    private String countryCode;
    private String sourceCode;
    private String subjectCode;
    private String trend;
    private Integer rankDelta;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public java.math.BigDecimal getScore() { return score; }
    public void setScore(java.math.BigDecimal v) { this.score = v; }
    public Short getYear() { return year; }
    public void setYear(Short v) { this.year = v; }
    public String getUrlId() { return urlId; }
    public void setUrlId(String v) { this.urlId = v; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String v) { this.nameEn = v; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String v) { this.nameZh = v; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String v) { this.logoUrl = v; }
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String v) { this.regionCode = v; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String v) { this.countryCode = v; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String v) { this.sourceCode = v; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String v) { this.subjectCode = v; }
    public String getTrend() { return trend; }
    public void setTrend(String v) { this.trend = v; }
    public Integer getRankDelta() { return rankDelta; }
    public void setRankDelta(Integer v) { this.rankDelta = v; }
}
