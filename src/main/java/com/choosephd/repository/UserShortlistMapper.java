package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.UserShortlist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户选校清单 mapper — 登录用户专属。
 *
 * <p>继承 {@code BaseMapper<UserShortlist>}。所有 SQL 都必带
 * {@code user_id = #{userId}} 过滤（service 层强制传 userId，
 * 防止越权访问他人选校）。
 */
@Mapper
public interface UserShortlistMapper extends BaseMapper<UserShortlist> {
}
