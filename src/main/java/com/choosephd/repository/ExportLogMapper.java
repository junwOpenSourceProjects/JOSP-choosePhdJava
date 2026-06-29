package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.ExportLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExportLogMapper extends BaseMapper<ExportLog> {

    @Select("SELECT COUNT(*) FROM export_log WHERE user_id = #{userId} AND created_at >= #{since}")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);
}
