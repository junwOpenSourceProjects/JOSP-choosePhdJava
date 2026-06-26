package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.RankingSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 排名榜单 source mapper — 榜单字典（QS/USNews/THE 等）。
 *
 * <p>继承 {@code BaseMapper<RankingSource>}。
 *
 * <p>自定义方法：
 * <ul>
 *   <li>{@code listAll()} — admin 端拉所有 source（含 active=0）</li>
 *   <li>{@code listActive()} — 公开端只拉 active=1</li>
 *   <li>{@code findBySourceName(sourceName)} — 数据导入时按名查 source</li>
 * </ul>
 */
@Mapper
public interface RankingSourceMapper extends BaseMapper<RankingSource> {

    @Select("SELECT * FROM ranking_source WHERE slug = #{slug} LIMIT 1")
    RankingSource selectBySlug(@Param("slug") String slug);
}
