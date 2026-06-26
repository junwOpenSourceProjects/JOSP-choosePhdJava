package com.choosephd.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.choosephd.common.BusinessException;
import com.choosephd.common.PageResult;
import com.choosephd.dto.PageQuery;
import com.choosephd.dto.PageUtil;
import com.choosephd.dto.RankingEntryVo;
import com.choosephd.entity.RankingEntry;
import com.choosephd.entity.RankingSource;
import com.choosephd.repository.RankingEntryMapper;
import com.choosephd.repository.RankingSourceMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排名榜单 source + entry 数据访问 service。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@code listActiveSources()} — admin 后台拉所有 active source</li>
 *   <li>{@code listRankingsBySource(sourceId, page)} — 单榜单下分页查 entry（按 rankValue 升序）</li>
 *   <li>{@code listEntriesByUniversity(universityId)} — 单院校历年所有榜单 entry</li>
 * </ul>
 *
 * <p>所有写操作集中在 admin controller（{@link com.choosephd.controller.v1.RankingSourceAdminController}），
 * 本 service 只做查询；权限校验在 controller 入口。
 */
@Service
public class RankingSourceService {

    private final RankingSourceMapper rankingSourceMapper;
    private final RankingEntryMapper rankingEntryMapper;

    public RankingSourceService(RankingSourceMapper rankingSourceMapper, RankingEntryMapper rankingEntryMapper) {
        this.rankingSourceMapper = rankingSourceMapper;
        this.rankingEntryMapper = rankingEntryMapper;
    }

    public PageResult<RankingSource> listSources(Integer kind, String ownerOrg, PageQuery query) {
        QueryWrapper<RankingSource> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (kind != null) {
            wrapper.eq("kind", kind);
        }
        if (ownerOrg != null && !ownerOrg.isBlank()) {
            wrapper.eq("owner_org", ownerOrg.trim());
        }
        wrapper.orderByAsc("id");
        IPage<RankingSource> page = rankingSourceMapper.selectPage(PageUtil.toPage(query), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public RankingSource getSource(Integer id) {
        RankingSource source = rankingSourceMapper.selectById(id);
        if (source == null) {
            throw new BusinessException("排名来源不存在");
        }
        return source;
    }

    public PageResult<RankingEntryVo> listSourceEntries(Integer sourceId, Integer year, Long page, Long size) {
        RankingSource source = rankingSourceMapper.selectById(sourceId);
        if (source == null) {
            throw new BusinessException("排名来源不存在");
        }
        IPage<RankingEntryVo> result = rankingEntryMapper.selectVoPageBySource(
                PageUtil.toPage(page, size, 200), sourceId, year);
        List<RankingEntryVo> records = result.getRecords();
        if (year != null && !records.isEmpty()) {
            fillRankDelta(records, sourceId, year);
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<Integer> listSourceYears(Integer sourceId) {
        RankingSource source = rankingSourceMapper.selectById(sourceId);
        if (source == null) {
            throw new BusinessException("排名来源不存在");
        }
        return rankingEntryMapper.selectYearsBySource(sourceId);
    }

    private void fillRankDelta(List<RankingEntryVo> entries, Integer sourceId, Integer year) {
        IPage<RankingEntryVo> prevResult = rankingEntryMapper.selectVoPageBySource(
                PageUtil.toPage(1L, 10000L, 200), sourceId, year - 1);
        List<RankingEntryVo> prevRecords = prevResult.getRecords();
        if (prevRecords == null) {
            prevRecords = Collections.emptyList();
        }
        Map<String, RankingEntryVo> prevMap = prevRecords
                .stream()
                .filter(r -> r.getUniversityId() != null)
                .collect(Collectors.toMap(
                        r -> r.getUniversityId() + "#" + (r.getSubjectId() == null ? "" : r.getSubjectId()),
                        r -> r,
                        (a, b) -> a));
        for (RankingEntryVo entry : entries) {
            if (entry.getRankDelta() != null) {
                continue;
            }
            String key = entry.getUniversityId() + "#" + (entry.getSubjectId() == null ? "" : entry.getSubjectId());
            RankingEntryVo prev = prevMap.get(key);
            if (prev != null && prev.getRankValue() != null && entry.getRankValue() != null) {
                entry.setRankDelta(entry.getRankValue() - prev.getRankValue());
            }
        }
    }
}
