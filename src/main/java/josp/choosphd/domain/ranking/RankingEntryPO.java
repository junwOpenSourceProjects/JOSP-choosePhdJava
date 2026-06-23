package josp.choosphd.domain.ranking;

import java.time.LocalDateTime;

/**
 * ranking_entry 事实表(V1 schema: id PK, university_id/source_id/subject_id FK)
 */
public class RankingEntryPO {
    private Long id;
    private Long universityId;
    private Integer sourceId;
    private Integer subjectId;
    private Short year;
    private Integer rankDisplay;
    private java.math.BigDecimal rankExact;
    private java.math.BigDecimal score;
    private String indicators;          // JSON
    private String sourceRawId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long v) { this.universityId = v; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer v) { this.sourceId = v; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer v) { this.subjectId = v; }
    public Short getYear() { return year; }
    public void setYear(Short v) { this.year = v; }
    public Integer getRankDisplay() { return rankDisplay; }
    public void setRankDisplay(Integer v) { this.rankDisplay = v; }
    public java.math.BigDecimal getRankExact() { return rankExact; }
    public void setRankExact(java.math.BigDecimal v) { this.rankExact = v; }
    public java.math.BigDecimal getScore() { return score; }
    public void setScore(java.math.BigDecimal v) { this.score = v; }
    public String getIndicators() { return indicators; }
    public void setIndicators(String v) { this.indicators = v; }
    public String getSourceRawId() { return sourceRawId; }
    public void setSourceRawId(String v) { this.sourceRawId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer v) { this.deleted = v; }
}
