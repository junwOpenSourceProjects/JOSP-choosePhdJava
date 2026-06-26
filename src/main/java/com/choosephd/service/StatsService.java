package com.choosephd.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.choosephd.dto.StatsOverviewResponse;
import com.choosephd.entity.RankingEntry;
import com.choosephd.entity.RankingSource;
import com.choosephd.entity.Subject;
import com.choosephd.entity.University;
import com.choosephd.repository.RankingEntryMapper;
import com.choosephd.repository.RankingSourceMapper;
import com.choosephd.repository.SubjectMapper;
import com.choosephd.repository.UniversityMapper;
import org.springframework.stereotype.Service;

/**
 * 首页 Dashboard 聚合统计 service。
 *
 * <p>返回 {@link com.choosephd.dto.StatsOverviewResponse}：院校总数 / 榜单总数 /
 * 排名条目总数 / 学科总数 / 最近更新时间。所有数据从 MyBatis-Plus count 查询直接返回，
 * 不做复杂聚合 (单字段 count 走索引，毫秒级)。
 *
 * <p>Controller 入口：{@link com.choosephd.controller.v1.StatsController#getOverview}。
 */
@Service
public class StatsService {

    private final UniversityMapper universityMapper;
    private final RankingEntryMapper rankingEntryMapper;
    private final RankingSourceMapper rankingSourceMapper;
    private final SubjectMapper subjectMapper;

    public StatsService(UniversityMapper universityMapper,
                        RankingEntryMapper rankingEntryMapper,
                        RankingSourceMapper rankingSourceMapper,
                        SubjectMapper subjectMapper) {
        this.universityMapper = universityMapper;
        this.rankingEntryMapper = rankingEntryMapper;
        this.rankingSourceMapper = rankingSourceMapper;
        this.subjectMapper = subjectMapper;
    }

    public StatsOverviewResponse overview() {
        long universityCount = universityMapper.selectCount(new QueryWrapper<University>().eq("deleted", 0));
        long rankingEntryCount = rankingEntryMapper.selectCount(new QueryWrapper<RankingEntry>().eq("deleted", 0));
        long rankingSourceCount = rankingSourceMapper.selectCount(new QueryWrapper<RankingSource>().eq("deleted", 0));
        long subjectCount = subjectMapper.selectCount(new QueryWrapper<Subject>().eq("deleted", 0));
        return new StatsOverviewResponse(universityCount, rankingEntryCount, rankingSourceCount, subjectCount);
    }
}
