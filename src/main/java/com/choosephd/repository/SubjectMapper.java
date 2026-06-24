package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SubjectMapper extends BaseMapper<Subject> {

    @Select("SELECT * FROM subject WHERE slug = #{slug} LIMIT 1")
    Subject selectBySlug(@Param("slug") String slug);
}
