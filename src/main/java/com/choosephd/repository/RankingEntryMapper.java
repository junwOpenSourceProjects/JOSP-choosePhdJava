package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.choosephd.dto.RankingEntryVo;
import com.choosephd.dto.UniversitySourceSummary;
import com.choosephd.entity.RankingEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 排名条目 mapper — 单榜单分页 / 单院校历年 / 趋势聚合。
 *
 * <p>继承 {@code BaseMapper<RankingEntry>}。自定义 XML SQL 写在
 * {@code resources/mapper/RankingEntryMapper.xml}。
 *
 * <p>自定义方法：
 * <ul>
 *   <li>{@code listBySource(Page, sourceId, includeSubject)} — 单榜单下分页查 entry</li>
 *   <li>{@code listByUniversity(universityId)} — 单院校所有榜单 entry（历年）</li>
 *   <li>{@code aggregateBySource(sourceId)} — 单榜单 best/worst/year 聚合</li>
 * </ul>
 *
 * <p>性能要点：ranking_entry 表 90w+ 行，必走 (source_id, rank_value) /
 * (university_id, year) 复合索引。
 */
@Mapper
public interface RankingEntryMapper extends BaseMapper<RankingEntry> {

    int deleteByNaturalKey(@Param("universityId") String universityId,
                           @Param("sourceId") Integer sourceId,
                           @Param("subjectId") Integer subjectId,
                           @Param("year") Integer year);

    IPage<RankingEntryVo> selectVoPageBySource(IPage<RankingEntryVo> page,
                                                @Param("sourceId") Integer sourceId,
                                                @Param("year") Integer year);

    IPage<RankingEntryVo> selectVoPageByUniversity(IPage<RankingEntryVo> page,
                                                    @Param("universityId") String universityId,
                                                    @Param("sourceId") Integer sourceId,
                                                    @Param("year") Integer year,
                                                    @Param("overallOnly") Boolean overallOnly);

    List<UniversitySourceSummary> selectSourceSummariesByUniversity(@Param("universityId") String universityId);

    List<Integer> selectYearsBySource(@Param("sourceId") Integer sourceId);
}
