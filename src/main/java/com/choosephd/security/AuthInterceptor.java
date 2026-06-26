package com.choosephd.security;

import com.choosephd.common.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权拦截器 — 校验 JWT 并把 userId/role 写入 request attribute。
 *
 * <p>挂载在 {@code /api/v1/**} 下（白名单除外）— 见 {@link com.choosephd.config.WebConfig#addInterceptors}。
 *
 * <p>失败时按项目统一 {@link ApiResult} 格式返 JSON，避免前端 fetch().json() 拿到 status 401
 * 但 body 空无法解析原因的情况。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检直接放行 — CORS 由 WebConfig.addCorsMappings 处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "缺少 Authorization Bearer token");
            return false;
        }
        String token = authHeader.substring(7);
        Claims claims;
        try {
            if (!jwtService.validateToken(token)) {
                writeUnauthorized(response, "token 无效或已过期");
                return false;
            }
            claims = jwtService.parseToken(token);
        } catch (Exception e) {
            writeUnauthorized(response, "token 解析失败");
            return false;
        }
        try {
            request.setAttribute("userId", Long.valueOf(claims.getSubject()));
            request.setAttribute("role", claims.get("role", String.class));
        } catch (NumberFormatException e) {
            writeUnauthorized(response, "token subject 非法");
            return false;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResult<Void> body = ApiResult.error(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
