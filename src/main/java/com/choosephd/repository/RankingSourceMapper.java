package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.RankingSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RankingSourceMapper extends BaseMapper<RankingSource> {

    @Select("SELECT * FROM ranking_source WHERE slug = #{slug} LIMIT 1")
    RankingSource selectBySlug(@Param("slug") String slug);
}
