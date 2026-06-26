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
     * 一次 SQL 返回 (urlId, tagVo) 对，前端在 service 层按 urlId 分组
     */
    @Select("""
            SELECT r.university_id AS university_id,
                   t.id, t.slug, t.name_zh, t.name_en, t.category, t.color, t.description, t.sort_order
            FROM university_tag_relation r
            JOIN university_tag t ON t.id = r.tag_id
            WHERE r.university_id IN
                <foreach collection='universityIds' item='uid' open='(' separator=',' close=')'>
                    #{uid}
                </foreach>
              AND t.active = 1
              AND t.deleted = 0
            ORDER BY r.university_id, t.sort_order ASC, t.id ASC
            """)
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
