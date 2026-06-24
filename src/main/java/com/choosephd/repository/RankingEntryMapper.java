package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.choosephd.dto.RankingEntryVo;
import com.choosephd.dto.UniversitySourceSummary;
import com.choosephd.entity.RankingEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
