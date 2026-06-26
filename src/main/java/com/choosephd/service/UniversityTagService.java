package com.choosephd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.choosephd.common.BusinessException;
import com.choosephd.dto.UniversityTagVo;
import com.choosephd.entity.UniversityTag;
import com.choosephd.entity.UniversityTagRelation;
import com.choosephd.repository.UniversityTagMapper;
import com.choosephd.repository.UniversityTagRelationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UniversityTagService {

    private final UniversityTagMapper universityTagMapper;
    private final UniversityTagRelationMapper relationMapper;

    public UniversityTagService(UniversityTagMapper universityTagMapper, UniversityTagRelationMapper relationMapper) {
        this.universityTagMapper = universityTagMapper;
        this.relationMapper = relationMapper;
    }

    public List<UniversityTagVo> listActiveTags() {
        return universityTagMapper.selectAllActiveTags();
    }

    public List<UniversityTagVo> listTagsByUniversity(String universityId) {
        if (universityId == null || universityId.isBlank()) {
            return Collections.emptyList();
        }
        return universityTagMapper.selectTagsByUniversity(universityId);
    }

    public Set<String> listUniversityIdsByTag(Integer tagId) {
        if (tagId == null) {
            return Collections.emptySet();
        }
        return universityTagMapper.selectUniversityIdsByTagId(tagId);
    }

    @Transactional
    public void assignTag(String universityId, Integer tagId) {
        if (universityId == null || universityId.isBlank() || tagId == null) {
            throw new BusinessException("参数错误");
        }
        UniversityTag tag = universityTagMapper.selectById(tagId);
        if (tag == null || tag.getDeleted() != null && tag.getDeleted() == 1) {
            throw new BusinessException("标签不存在");
        }
        LambdaQueryWrapper<UniversityTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UniversityTagRelation::getUniversityId, universityId)
                .eq(UniversityTagRelation::getTagId, tagId);
        if (relationMapper.selectCount(wrapper) > 0) {
            return;
        }
        UniversityTagRelation relation = new UniversityTagRelation();
        relation.setUniversityId(universityId);
        relation.setTagId(tagId);
        relationMapper.insert(relation);
    }

    @Transactional
    public void removeTag(String universityId, Integer tagId) {
        if (universityId == null || universityId.isBlank() || tagId == null) {
            return;
        }
        LambdaQueryWrapper<UniversityTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UniversityTagRelation::getUniversityId, universityId)
                .eq(UniversityTagRelation::getTagId, tagId);
        relationMapper.delete(wrapper);
    }

    @Transactional
    public void setUniversityTags(String universityId, List<Integer> tagIds) {
        if (universityId == null || universityId.isBlank()) {
            throw new BusinessException("参数错误");
        }
        List<Integer> safeTagIds = tagIds == null ? Collections.emptyList() : tagIds.stream()
                .distinct()
                .filter(id -> id != null)
                .collect(Collectors.toList());

        LambdaQueryWrapper<UniversityTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UniversityTagRelation::getUniversityId, universityId);
        relationMapper.delete(wrapper);

        for (Integer tagId : safeTagIds) {
            UniversityTagRelation relation = new UniversityTagRelation();
            relation.setUniversityId(universityId);
            relation.setTagId(tagId);
            relationMapper.insert(relation);
        }
    }
}
