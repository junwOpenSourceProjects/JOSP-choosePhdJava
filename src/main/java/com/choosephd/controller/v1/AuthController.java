package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.dto.AuthRequest;
import com.choosephd.dto.AuthResponse;
import com.choosephd.dto.UserInfo;
import com.choosephd.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

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
