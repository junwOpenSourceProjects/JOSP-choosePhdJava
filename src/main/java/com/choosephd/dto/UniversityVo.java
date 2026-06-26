package com.choosephd.dto;

import com.choosephd.entity.University;

import java.util.List;

public class UniversityVo extends University {

    private List<UniversityTagVo> tags;

    public List<UniversityTagVo> getTags() {
        return tags;
    }

    public void setTags(List<UniversityTagVo> tags) {
        this.tags = tags;
    }
}
