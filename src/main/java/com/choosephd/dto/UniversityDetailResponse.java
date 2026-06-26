package com.choosephd.dto;

import com.choosephd.entity.University;

import java.util.List;

public class UniversityDetailResponse {

    private University university;
    private List<UniversitySourceSummary> sources;
    private List<UniversityTagVo> tags;

    public UniversityDetailResponse() {
    }

    public UniversityDetailResponse(University university, List<UniversitySourceSummary> sources) {
        this.university = university;
        this.sources = sources;
    }

    public University getUniversity() {
        return university;
    }

    public void setUniversity(University university) {
        this.university = university;
    }

    public List<UniversitySourceSummary> getSources() {
        return sources;
    }

    public void setSources(List<UniversitySourceSummary> sources) {
        this.sources = sources;
    }

    public List<UniversityTagVo> getTags() {
        return tags;
    }

    public void setTags(List<UniversityTagVo> tags) {
        this.tags = tags;
    }
}
