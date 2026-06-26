package com.choosephd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.choosephd.common.BusinessException;
import com.choosephd.dto.UniversityTagRequest;
import com.choosephd.dto.UniversityTagVo;
import com.choosephd.dto.UniversityTagWithUniversity;
import com.choosephd.entity.UniversityTag;
import com.choosephd.entity.UniversityTagRelation;
import com.choosephd.repository.UniversityTagMapper;
import com.choosephd.repository.UniversityTagRelationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * 批量按多所大学查标签 — 一次 SQL 解决 N+1。
     * 返回 Map<universityId, List<UniversityTagVo>>，没标签的大学不在 Map 里（调用方 getOrDefault 空列表）。
     */
    public Map<String, List<UniversityTagVo>> listTagsByUniversities(java.util.Collection<String> universityIds) {
        if (universityIds == null || universityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UniversityTagWithUniversity> rows = universityTagMapper.selectTagsByUniversityIds(universityIds);
        Map<String, List<UniversityTagVo>> result = new HashMap<>();
        for (UniversityTagWithUniversity row : rows) {
            UniversityTagVo vo = new UniversityTagVo();
            vo.setId(row.getId());
            vo.setSlug(row.getSlug());
            vo.setNameZh(row.getNameZh());
            vo.setNameEn(row.getNameEn());
            vo.setCategory(row.getCategory());
            vo.setColor(row.getColor());
            vo.setDescription(row.getDescription());
            vo.setSortOrder(row.getSortOrder());
            result.computeIfAbsent(row.getUniversityId(), k -> new java.util.ArrayList<>()).add(vo);
        }
        return result;
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

        if (!safeTagIds.isEmpty()) {
            LambdaQueryWrapper<UniversityTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.in(UniversityTag::getId, safeTagIds)
                    .eq(UniversityTag::getDeleted, 0);
            long validCount = universityTagMapper.selectCount(tagWrapper);
            if (validCount != safeTagIds.size()) {
                throw new BusinessException("存在无效或已删除的标签");
            }
        }

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

    public List<UniversityTag> listAllTags() {
        LambdaQueryWrapper<UniversityTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UniversityTag::getDeleted, 0)
                .orderByAsc(UniversityTag::getSortOrder)
                .orderByAsc(UniversityTag::getId);
        return universityTagMapper.selectList(wrapper);
    }

    public UniversityTag getTagById(Integer id) {
        if (id == null) {
            return null;
        }
        UniversityTag tag = universityTagMapper.selectById(id);
        return tag != null && tag.getDeleted() != null && tag.getDeleted() == 1 ? null : tag;
    }

    @Transactional
    public UniversityTag createTag(UniversityTagRequest request) {
        if (request.getSlug() == null || request.getSlug().isBlank()
                || request.getNameZh() == null || request.getNameZh().isBlank()) {
            throw new BusinessException("slug 和中文名称不能为空");
        }
        LambdaQueryWrapper<UniversityTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UniversityTag::getSlug, request.getSlug().trim());
        if (universityTagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("标签标识已存在");
        }
        UniversityTag tag = new UniversityTag();
        tag.setSlug(request.getSlug().trim());
        tag.setNameZh(request.getNameZh().trim());
        tag.setNameEn(request.getNameEn());
        tag.setCategory(request.getCategory());
        tag.setColor(request.getColor());
        tag.setDescription(request.getDescription());
        tag.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        tag.setActive(request.getActive() == null ? 1 : request.getActive());
        universityTagMapper.insert(tag);
        return tag;
    }

    @Transactional
    public UniversityTag updateTag(Integer id, UniversityTagRequest request) {
        UniversityTag tag = getTagById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = request.getSlug().trim();
            if (!newSlug.equals(tag.getSlug())) {
                LambdaQueryWrapper<UniversityTag> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UniversityTag::getSlug, newSlug);
                if (universityTagMapper.selectCount(wrapper) > 0) {
                    throw new BusinessException("标签标识已存在");
                }
                tag.setSlug(newSlug);
            }
        }
        if (request.getNameZh() != null && !request.getNameZh().isBlank()) {
            tag.setNameZh(request.getNameZh().trim());
        }
        if (request.getNameEn() != null) {
            tag.setNameEn(request.getNameEn());
        }
        if (request.getCategory() != null) {
            tag.setCategory(request.getCategory());
        }
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            tag.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            tag.setActive(request.getActive());
        }
        universityTagMapper.updateById(tag);
        return tag;
    }

    @Transactional
    public void deleteTag(Integer id) {
        UniversityTag tag = getTagById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        LambdaQueryWrapper<UniversityTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UniversityTagRelation::getTagId, id);
        relationMapper.delete(wrapper);
        universityTagMapper.deleteById(id);
    }
}
