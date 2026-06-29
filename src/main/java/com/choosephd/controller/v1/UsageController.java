package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户配额使用情况查询端点 — 前端据此展示剩余额度。
 */
@RestController
@RequestMapping("/api/v1/usage")
public class UsageController {

    private final JwtService jwtService;

    public UsageController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> myUsage(HttpServletRequest request) {
        String membership = "free";
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                if (jwtService.validateToken(token)) {
                    Claims claims = jwtService.parseToken(token);
                    membership = claims.get("membership", String.class);
                    if (membership == null) membership = "free";
                }
            } catch (Exception ignored) {
            }
        }

        boolean isPaid = !"free".equals(membership);
        return ApiResult.ok(Map.of(
                "membership", membership,
                "dailyListLimit", isPaid ? 5000 : 500,
                "dailyTotalLimit", isPaid ? 20000 : 2000,
                "pageSizeLimit", isPaid ? 100 : 30,
                "compareLimit", isPaid ? 10 : 3,
                "exportEnabled", "premium".equals(membership),
                "exportMonthlyLimit", "premium".equals(membership) ? 20 : 0
        ));
    }
}
