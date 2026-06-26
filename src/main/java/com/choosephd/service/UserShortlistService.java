package com.choosephd.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.choosephd.common.BusinessException;
import com.choosephd.dto.ShortlistItemVo;
import com.choosephd.dto.ShortlistRequest;
import com.choosephd.entity.University;
import com.choosephd.entity.UserShortlist;
import com.choosephd.repository.UniversityMapper;
import com.choosephd.repository.UserShortlistMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户选校清单 service — 加/删/查/排序四入口。
 *
 * <p>每个登录用户维护一份选校清单（每条目含 universityId + priority + note + addedAt），
 * 用于"对比"页和"我的选校"页的预选数据。
 *
 * <p>事务边界：upsertShortlistItem（用户加或改优先级）走 @Transactional，
 * 删除/查询无事务。
 *
 * <p>Controller 入口：{@link com.choosephd.controller.v1.UserShortlistController}。
 */
@Service
public class UserShortlistService {

    private final UserShortlistMapper userShortlistMapper;
    private final UniversityMapper universityMapper;

    public UserShortlistService(UserShortlistMapper userShortlistMapper, UniversityMapper universityMapper) {
        this.userShortlistMapper = userShortlistMapper;
        this.universityMapper = universityMapper;
    }

    public List<ShortlistItemVo> getShortlist(Long userId) {
        QueryWrapper<UserShortlist> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("deleted", 0);
        wrapper.orderByAsc("priority").orderByDesc("added_at");
        List<UserShortlist> items = userShortlistMapper.selectList(wrapper);
        return enrich(items);
    }

    @Transactional
    public ShortlistItemVo upsertShortlist(Long userId, ShortlistRequest request) {
        if (request.getUniversityId() == null || request.getUniversityId().isBlank()) {
            throw new BusinessException("大学ID不能为空");
        }
        String universityId = request.getUniversityId().trim();
        University university = universityMapper.selectById(universityId);
        if (university == null) {
            throw new BusinessException("大学不存在");
        }

        Integer priority = request.getPriority();
        if (priority == null) {
            priority = 2;
        }
        if (priority < 1 || priority > 4) {
            throw new BusinessException("优先级必须在1-4之间");
        }

        QueryWrapper<UserShortlist> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("university_id", universityId).eq("deleted", 0);
        UserShortlist existing = userShortlistMapper.selectOne(wrapper);

        LocalDateTime now = LocalDateTime.now();
        UserShortlist item;
        if (existing != null) {
            item = existing;
            item.setPriority(priority);
            item.setNote(request.getNote());
            item.setUpdatedAt(now);
            userShortlistMapper.updateById(item);
        } else {
            item = new UserShortlist();
            item.setUserId(userId);
            item.setUniversityId(universityId);
            item.setPriority(priority);
            item.setNote(request.getNote());
            item.setAddedAt(now);
            item.setUpdatedAt(now);
            userShortlistMapper.insert(item);
        }

        ShortlistItemVo vo = toVo(item);
        vo.setUniversity(university);
        return vo;
    }

    @Transactional
    public void deleteShortlist(Long userId, String universityId) {
        QueryWrapper<UserShortlist> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("university_id", universityId).eq("deleted", 0);
        UserShortlist existing = userShortlistMapper.selectOne(wrapper);
        if (existing != null) {
            userShortlistMapper.deleteById(existing.getId());
        }
    }

    private List<ShortlistItemVo> enrich(List<UserShortlist> items) {
        if (items.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> universityIds = items.stream()
                .map(UserShortlist::getUniversityId)
                .collect(Collectors.toSet());
        List<University> universities = universityMapper.selectBatchIds(universityIds);
        Map<String, University> universityMap = universities.stream()
                .collect(Collectors.toMap(University::getUrlId, u -> u));

        List<ShortlistItemVo> result = new ArrayList<>();
        for (UserShortlist item : items) {
            ShortlistItemVo vo = toVo(item);
            vo.setUniversity(universityMap.get(item.getUniversityId()));
            result.add(vo);
        }
        return result;
    }

    private ShortlistItemVo toVo(UserShortlist item) {
        ShortlistItemVo vo = new ShortlistItemVo();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setUniversityId(item.getUniversityId());
        vo.setPriority(item.getPriority());
        vo.setNote(item.getNote());
        vo.setAddedAt(item.getAddedAt());
        vo.setUpdatedAt(item.getUpdatedAt());
        return vo;
    }
}
