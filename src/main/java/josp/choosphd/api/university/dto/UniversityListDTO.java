package josp.choosphd.api.university.dto;

public class UniversityListDTO {
    private String urlId;
    private String name;
    private String cnName;
    private String region;
    private String country;
    private String logo;
    private Integer latestRank;
    private Double latestScore;
    private Integer latestYear;
    private String sourceCode;
    private String trend;
    private Integer rankDelta;

    public String getUrlId() { return urlId; }
    public void setUrlId(String v) { this.urlId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCnName() { return cnName; }
    public void setCnName(String v) { this.cnName = v; }
    public String getRegion() { return region; }
    public void setRegion(String v) { this.region = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }
    public String getLogo() { return logo; }
    public void setLogo(String v) { this.logo = v; }
    public Integer getLatestRank() { return latestRank; }
    public void setLatestRank(Integer v) { this.latestRank = v; }
    public Double getLatestScore() { return latestScore; }
    public void setLatestScore(Double v) { this.latestScore = v; }
    public Integer getLatestYear() { return latestYear; }
    public void setLatestYear(Integer v) { this.latestYear = v; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String v) { this.sourceCode = v; }
    public String getTrend() { return trend; }
    public void setTrend(String v) { this.trend = v; }
    public Integer getRankDelta() { return rankDelta; }
    public void setRankDelta(Integer v) { this.rankDelta = v; }
}
