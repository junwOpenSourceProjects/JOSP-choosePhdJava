package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.dto.AuthRequest;
import com.choosephd.dto.AuthResponse;
import com.choosephd.dto.UserInfo;
import com.choosephd.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 controller — 注册/登录/me 三个端点。
 *
 * <p>无鉴权要求（注册登录本来就要匿名访问），所以本 controller 不走
 * {@link com.choosephd.security.AuthInterceptor} 拦截逻辑之外的特殊处理。
 *
 * <p>权限：注册/登录/me 都匿名可访问；JWT 在登录成功后通过 response body 返回给前端，
 * 前端存 cookie 后再调其它鉴权接口。
 *
 * <p>Service：{@link com.choosephd.service.AuthService}。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResult<AuthResponse> register(@RequestBody AuthRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResult<AuthResponse> login(@RequestBody AuthRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResult<UserInfo> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.ok(authService.getMe(userId));
    }
}
