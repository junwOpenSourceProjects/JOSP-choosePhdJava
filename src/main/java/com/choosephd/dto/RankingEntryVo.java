package com.choosephd.dto;

public class RankingEntryVo {

    private Long id;
    private String universityId;
    private String universityNameZh;
    private String universityNameEn;
    private String country;
    private Integer sourceId;
    private String sourceName;
    private Integer year;
    private String rankDisplay;
    private Integer rankValue;
    private Integer rankDelta;
    private Integer direction;
    private Integer subjectId;
    private String subjectName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniversityId() {
        return universityId;
    }

    public void setUniversityId(String universityId) {
        this.universityId = universityId;
    }

    public String getUniversityNameZh() {
        return universityNameZh;
    }

    public void setUniversityNameZh(String universityNameZh) {
        this.universityNameZh = universityNameZh;
    }

    public String getUniversityNameEn() {
        return universityNameEn;
    }

    public void setUniversityNameEn(String universityNameEn) {
        this.universityNameEn = universityNameEn;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getRankDisplay() {
        return rankDisplay;
    }

    public void setRankDisplay(String rankDisplay) {
        this.rankDisplay = rankDisplay;
    }

    public Integer getRankValue() {
        return rankValue;
    }

    public void setRankValue(Integer rankValue) {
        this.rankValue = rankValue;
    }

    public Integer getRankDelta() {
        return rankDelta;
    }

    public void setRankDelta(Integer rankDelta) {
        this.rankDelta = rankDelta;
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
