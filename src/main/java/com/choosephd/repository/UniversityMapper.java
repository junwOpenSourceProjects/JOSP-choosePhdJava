package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.choosephd.dto.UniversityVo;
import com.choosephd.entity.University;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 院校核心 mapper — 列表分页/详情/标签批量/国家 distinct 等。
 *
 * <p>继承 {@code BaseMapper<University>}。自定义 XML SQL 写在
 * {@code resources/mapper/UniversityMapper.xml}。
 *
 * <p>自定义方法：
 * <ul>
 *   <li>{@code selectSearchPage(Page, PageQuery)} — 列表分页（关键字/国家/地区/标签筛选 + 排序）</li>
 *   <li>{@code selectCountries(PageQuery)} — distinct country 列表（筛选用）</li>
 *   <li>{@code selectByUrlId(urlId)} — 单院校详情（不含榜单 entry）</li>
 *   <li>{@code listByUrlIds(urlIds)} — 批量查 N 校基本信息（取代 N+1）</li>
 * </ul>
 *
 * <p>性能要点：selectSearchPage 走 best_rank_value LEFT JOIN + tag multi-JOIN，
 * 大数据量场景 (10000+ 校) 需要补 (source_id, rank_value) 复合索引。
 */
@Mapper
public interface UniversityMapper extends BaseMapper<University> {

    IPage<UniversityVo> selectSearchPage(
            IPage<UniversityVo> page,
            @Param("keyword") String keyword,
            @Param("continent") String continent,
            @Param("continentCountries") Set<String> continentCountries,
            @Param("country") String country,
            @Param("sortBy") String sortBy,
            @Param("tagIds") List<Integer> tagIds);

    List<String> selectCountries(
            @Param("continent") String continent,
            @Param("continentCountries") Set<String> continentCountries);
}
