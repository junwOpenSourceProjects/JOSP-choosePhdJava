package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户账号 mapper — 注册 / 登录 / me 三个入口的数据访问。
 *
 * <p>继承 {@code BaseMapper<UserAccount>}。
 *
 * <p>自定义方法：
 * <ul>
 *   <li>{@code findByUsername(username)} — 登录时按用户名查（带 password_hash）</li>
 *   <li>{@code existsByUsername(username)} — 注册时查重</li>
 *   <li>{@code findUserInfo(userId)} — me 端点查公开信息（不含密码 hash）</li>
 * </ul>
 *
 * <p>安全：password_hash 字段严禁返给前端，findUserInfo 用单独的 resultType 过滤掉。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT * FROM user_account WHERE username = #{username} LIMIT 1")
    UserAccount selectByUsername(@Param("username") String username);
}
