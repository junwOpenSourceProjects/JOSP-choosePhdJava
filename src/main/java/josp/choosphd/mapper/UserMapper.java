package josp.choosphd.mapper;

import josp.choosphd.domain.auth.UserPO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    @Select("""
        SELECT id, username, password_hash AS passwordHash, display_name AS displayName,
               role, enabled, last_login_at AS lastLoginAt,
               created_at AS createdAt, updated_at AS updatedAt, deleted
        FROM users WHERE username = #{username} AND deleted = 0 LIMIT 1
    """)
    UserPO findByUsername(String username);

    @Insert("""
        INSERT INTO users (username, password_hash, display_name, role, enabled, created_at)
        VALUES (#{username}, #{passwordHash}, #{displayName}, #{role}, #{enabled}, #{createdAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserPO u);

    @Update("UPDATE users SET last_login_at = #{lastLoginAt} WHERE id = #{id}")
    int updateLastLogin(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime ts);
}
