package com.choosephd.service;

import com.choosephd.common.BusinessException;
import com.choosephd.dto.AuthRequest;
import com.choosephd.dto.AuthResponse;
import com.choosephd.dto.UserInfo;
import com.choosephd.entity.UserAccount;
import com.choosephd.repository.UserAccountMapper;
import com.choosephd.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户认证服务 — 注册/登录/me 三个核心入口。
 *
 * <p>负责：
 * <ul>
 *   <li>用户名/密码注册（密码 BCrypt 加密入库）</li>
 *   <li>用户名/密码登录（验证 BCrypt hash + 签发 JWT）</li>
 *   <li>me — 用 userId 查 user 公开信息（不含密码 hash）</li>
 * </ul>
 *
 * <p>鉴权链路下游：{@link com.choosephd.security.AuthInterceptor} 验 JWT 写
 * {@code request.setAttribute("userId", ...)}，本 service 不直接读 request attribute，
 * 由 controller 透传 userId 进来。
 */
@Service
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserAccountMapper userAccountMapper, JwtService jwtService) {
        this.userAccountMapper = userAccountMapper;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public AuthResponse register(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        String username = request.getUsername().trim();
        UserAccount existing = userAccountMapper.selectByUsername(username);
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.insert(user);

        String token = jwtService.generateToken(user.getId(), user.getRole(), user.getCreatedAt(),
                user.getMembership() != null ? user.getMembership() : "free");
        return new AuthResponse(token, toUserInfo(user));
    }

    public AuthResponse login(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        UserAccount user = userAccountMapper.selectByUsername(request.getUsername().trim());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtService.generateToken(user.getId(), user.getRole(), user.getCreatedAt(),
                user.getMembership() != null ? user.getMembership() : "free");
        return new AuthResponse(token, toUserInfo(user));
    }

    public UserInfo getMe(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserInfo(user);
    }

    private UserInfo toUserInfo(UserAccount user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getRole());
    }
}
