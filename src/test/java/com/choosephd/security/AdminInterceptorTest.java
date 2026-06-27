package com.choosephd.security;

import com.choosephd.common.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletOutputStream;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminInterceptor 单元测试 — 不启 Spring，纯 Mockito + Mock HttpServletRequest/Response。
 *
 * <p>覆盖 4 个分支：OPTIONS 放行 / ROLE_ADMIN 通过 / 未登录返 403 / 角色错返 403。
 */
class AdminInterceptorTest {

    private AdminInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new AdminInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        responseBody = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(responseBody));
    }

    @Test
    void preHandle_OPTIONS_alwaysAllowed() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void preHandle_roleAdmin_returnsTrue() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getAttribute(AdminInterceptor.ATTR_ROLE)).thenReturn(AdminInterceptor.ADMIN_ROLE);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void preHandle_roleMissing_returns403_withLoginMessage() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getAttribute(AdminInterceptor.ATTR_ROLE)).thenReturn(null);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        // 验证响应 body 是 ApiResult JSON
        ApiResult<?> body = new ObjectMapper().readValue(responseBody.toByteArray(), ApiResult.class);
        assertEquals(403, body.getCode());
        assertEquals("未登录或无角色", body.getMessage());
    }

    @Test
    void preHandle_roleNotAdmin_returns403_withNeedAdminMessage() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getAttribute(AdminInterceptor.ATTR_ROLE)).thenReturn("ROLE_USER");

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(403);

        ApiResult<?> body = new ObjectMapper().readValue(responseBody.toByteArray(), ApiResult.class);
        assertEquals(403, body.getCode());
        assertEquals("需要 admin 权限", body.getMessage());
    }
}
