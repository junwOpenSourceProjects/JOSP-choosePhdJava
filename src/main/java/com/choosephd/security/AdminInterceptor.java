package com.choosephd.security;

import com.choosephd.common.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Admin 角色拦截器 — 仅允许 {@code ROLE_ADMIN} 通过。
 *
 * <p>依赖 {@link AuthInterceptor} 先执行：必须登录且 JWT 合法，
 * 然后本拦截器从 request attribute {@code role} 读取角色。
 *
 * <p>失败时按 {@link ApiResult} JSON 返 403，避免前端 fetch 拿到空 body。
 *
 * <p>当前覆盖：{@code /api/v1/admin/**}（见 {@link com.choosephd.config.WebConfig#addInterceptors}）。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    /** admin 角色标识 — 与 {@link com.choosephd.config.AdminInitRunner} 创建的 admin 账号保持一致。 */
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    /** AuthInterceptor 写入 request attribute 的 role key — 见 {@link AuthInterceptor#preHandle}。 */
    public static final String ATTR_ROLE = "role";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Object role = request.getAttribute(ATTR_ROLE);
        if (role == null || !ADMIN_ROLE.equals(role.toString())) {
            writeForbidden(response, role == null ? "未登录或无角色" : "需要 admin 权限");
            return false;
        }
        return true;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResult<Void> body = ApiResult.error(403, message);
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}