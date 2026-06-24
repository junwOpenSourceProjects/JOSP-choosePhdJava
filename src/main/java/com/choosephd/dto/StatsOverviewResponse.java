package com.choosephd.dto;

public class StatsOverviewResponse {

    private Long universityCount;
    private Long rankingEntryCount;
    private Long rankingSourceCount;
    private Long subjectCount;

    public StatsOverviewResponse() {
    }

    public StatsOverviewResponse(Long universityCount, Long rankingEntryCount, Long rankingSourceCount, Long subjectCount) {
        this.universityCount = universityCount;
        this.rankingEntryCount = rankingEntryCount;
        this.rankingSourceCount = rankingSourceCount;
        this.subjectCount = subjectCount;
    }

    public Long getUniversityCount() {
        return universityCount;
    }

    public void setUniversityCount(Long universityCount) {
        this.universityCount = universityCount;
    }

    public Long getRankingEntryCount() {
        return rankingEntryCount;
    }

    public void setRankingEntryCount(Long rankingEntryCount) {
        this.rankingEntryCount = rankingEntryCount;
    }

    public Long getRankingSourceCount() {
        return rankingSourceCount;
    }

    public void setRankingSourceCount(Long rankingSourceCount) {
        this.rankingSourceCount = rankingSourceCount;
    }

    public Long getSubjectCount() {
        return subjectCount;
    }

    public void setSubjectCount(Long subjectCount) {
        this.subjectCount = subjectCount;
    }
}
