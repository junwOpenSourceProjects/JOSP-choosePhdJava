package josp.choosphd.api.university.dto;

/**
 * 院校列表查询参数(用于 ?q=...&region=... 等 query string 绑定)
 * 不需要 Lombok,用 record + setter 风格的 mutable wrapper
 */
public class UniversityListQuery {
    private String q;
    private String region;
    private String country;
    private String source;
    private Integer year;
    private String subject;
    private Integer rankMin;
    private Integer rankMax;
    private Double scoreMin;
    private Double scoreMax;
    private String trend;
    private String sortBy;
    private String sortDir;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Integer getRankMin() { return rankMin; }
    public void setRankMin(Integer rankMin) { this.rankMin = rankMin; }
    public Integer getRankMax() { return rankMax; }
    public void setRankMax(Integer rankMax) { this.rankMax = rankMax; }
    public Double getScoreMin() { return scoreMin; }
    public void setScoreMin(Double scoreMin) { this.scoreMin = scoreMin; }
    public Double getScoreMax() { return scoreMax; }
    public void setScoreMax(Double scoreMax) { this.scoreMax = scoreMax; }
    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortDir() { return sortDir; }
    public void setSortDir(String sortDir) { this.sortDir = sortDir; }
}
