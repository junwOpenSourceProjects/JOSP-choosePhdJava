package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学科 mapper — 学科字典表（init.sql 灌库，本 mapper 只读）。
 *
 * <p>继承 {@code BaseMapper<Subject>}。
 *
 * <p>自定义方法：
 * <ul>
 *   <li>{@code listAll()} — 公开端拉所有学科</li>
 * </ul>
 */
@Mapper
public interface SubjectMapper extends BaseMapper<Subject> {

    @Select("SELECT * FROM subject WHERE slug = #{slug} LIMIT 1")
    Subject selectBySlug(@Param("slug") String slug);
}
