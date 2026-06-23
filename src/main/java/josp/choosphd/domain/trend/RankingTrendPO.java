package josp.choosphd.domain.trend;

import java.time.LocalDateTime;

/**
 * ranking_trend 事实表(V1 schema: base_year / target_year / trend_type)
 */
public class RankingTrendPO {
    private Long id;
    private Long universityId;
    private Integer sourceId;
    private Integer subjectId;
    private String trendType;   // GROWING / DECLINING / STABLE
    private Short baseYear;
    private Short targetYear;
    private Integer rankChange;
    private Integer rankFrom;
    private Integer rankTo;
    private String note;
    private LocalDateTime createdAt;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long v) { this.universityId = v; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer v) { this.sourceId = v; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer v) { this.subjectId = v; }
    public String getTrendType() { return trendType; }
    public void setTrendType(String v) { this.trendType = v; }
    public Short getBaseYear() { return baseYear; }
    public void setBaseYear(Short v) { this.baseYear = v; }
    public Short getTargetYear() { return targetYear; }
    public void setTargetYear(Short v) { this.targetYear = v; }
    public Integer getRankChange() { return rankChange; }
    public void setRankChange(Integer v) { this.rankChange = v; }
    public Integer getRankFrom() { return rankFrom; }
    public void setRankFrom(Integer v) { this.rankFrom = v; }
    public Integer getRankTo() { return rankTo; }
    public void setRankTo(Integer v) { this.rankTo = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer v) { this.deleted = v; }
}
