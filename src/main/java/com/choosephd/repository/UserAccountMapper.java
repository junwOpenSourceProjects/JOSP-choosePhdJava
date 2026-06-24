package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT * FROM user_account WHERE username = #{username} LIMIT 1")
    UserAccount selectByUsername(@Param("username") String username);
}
