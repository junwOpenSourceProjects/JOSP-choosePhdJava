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

        String token = jwtService.generateToken(user.getId(), user.getRole());
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
        String token = jwtService.generateToken(user.getId(), user.getRole());
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
