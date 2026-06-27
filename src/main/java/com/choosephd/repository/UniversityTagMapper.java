package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.dto.UniversityTagVo;
import com.choosephd.dto.UniversityTagWithUniversity;
import com.choosephd.entity.UniversityTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 院校标签字典 mapper — admin 端 CRUD + 公开端 listActiveTags。
 *
 * <p>继承 {@code BaseMapper<UniversityTag>}，免费拿到 insert/update/delete/selectById 等基础方法。
 *
 * <p>自定义方法：
 * <ul>
 *   <li>{@code listAllTags()} — admin 端拉所有标签（含 active=0）</li>
 *   <li>{@code listActiveTags()} — 公开端只拉 active=1</li>
 *   <li>{@code listUniversitiesByTag(tagId)} — admin 端查某标签下所有院校</li>
 * </ul>
 */
@Mapper
public interface UniversityTagMapper extends BaseMapper<UniversityTag> {

    @Select("""
            SELECT t.id, t.slug, t.name_zh, t.name_en, t.category, t.color, t.description, t.sort_order
            FROM university_tag t
            JOIN university_tag_relation r ON r.tag_id = t.id
            WHERE r.university_id = #{universityId}
              AND t.active = 1
              AND t.deleted = 0
            ORDER BY t.sort_order ASC, t.id ASC
            """)
    List<UniversityTagVo> selectTagsByUniversity(@Param("universityId") String universityId);

    /**
     * 批量查询多所大学的标签 — 解决 N+1 查询
     * 一次 SQL 返回 (urlId, tagVo) 对，前端在 service 层按 urlId 分组。
     *
     * <p>实现位于 {@code mapper/UniversityTagMapper.xml}（MyBatis 注解不解析
     * {@code <foreach>} 等 XML 元素，必须用 XML mapper 才能跑动态 SQL）。
     */
    List<UniversityTagWithUniversity> selectTagsByUniversityIds(@Param("universityIds") java.util.Collection<String> universityIds);

    @Select("""
            SELECT t.id, t.slug, t.name_zh, t.name_en, t.category, t.color, t.description, t.sort_order
            FROM university_tag t
            WHERE t.active = 1
              AND t.deleted = 0
            ORDER BY t.sort_order ASC, t.id ASC
            """)
    List<UniversityTagVo> selectAllActiveTags();

    @Select("""
            SELECT DISTINCT r.university_id
            FROM university_tag_relation r
            JOIN university_tag t ON t.id = r.tag_id
            WHERE r.tag_id = #{tagId}
              AND t.active = 1
              AND t.deleted = 0
            """)
    Set<String> selectUniversityIdsByTagId(@Param("tagId") Integer tagId);
}
