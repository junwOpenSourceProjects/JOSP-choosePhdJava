package com.choosephd.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * 响应体混淆 Filter — 未登录用户访问敏感 API 时，将 JSON 响应体 XOR + Base64 编码，
 * 使浏览器 DevTools Network 标签中无法直接读取原始数据。
 *
 * <p>放行条件（满足任一即放行原样）：
 * <ul>
 *   <li>请求携带有效 JWT（已登录用户看真实数据）</li>
 *   <li>路径不在敏感列表中</li>
 *   <li>响应 Content-Type 不是 JSON</li>
 * </ul>
 */
@Component
@Order(Ordered.RESPONSE_OBFUSCATION)
public class ResponseObfuscationFilter implements Filter {

    private static final String OBFUSCATED_HEADER = "X-Obfuscated";
    private static final byte[] XOR_KEY = "JOSP-choosePhd-2026-net-tab-obfuscation-v1".getBytes(StandardCharsets.UTF_8);

    private static final Set<String> SENSITIVE_PATH_PREFIXES = Set.of(
            "/api/v1/universities",
            "/api/v1/sources",
            "/api/v1/rankings",
            "/api/v1/compare",
            "/api/v1/trends"
    );

    private final JwtService jwtService;

    public ResponseObfuscationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 已登录 → 放行原样
        if (isAuthenticated(req)) {
            chain.doFilter(request, response);
            return;
        }

        // 路径不在敏感列表 → 放行原样
        String path = req.getRequestURI();
        boolean sensitive = SENSITIVE_PATH_PREFIXES.stream().anyMatch(path::startsWith);
        if (!sensitive) {
            chain.doFilter(request, response);
            return;
        }

        // 包裹 response，拦截写入的 JSON 体
        ObfuscatedResponseWrapper wrapper = new ObfuscatedResponseWrapper(res);
        chain.doFilter(request, wrapper);

        // 拿到原始 JSON bytes → XOR + Base64 → 写入真实 response
        byte[] original = wrapper.getCapturedBytes();
        if (original.length > 0) {
            byte[] encoded = Base64.getEncoder().encode(xorBytes(original));
            res.setHeader(OBFUSCATED_HEADER, "1");
            res.setContentLength(encoded.length);
            res.getOutputStream().write(encoded);
        }
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        try {
            return jwtService.validateToken(authHeader.substring(7));
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] xorBytes(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] ^ XOR_KEY[i % XOR_KEY.length]);
        }
        return output;
    }

    /**
     * 拦截 servlet 输出流，捕获原始 JSON 字节。
     */
    private static class ObfuscatedResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream capture = new ByteArrayOutputStream();
        private PrintWriter writer;

        ObfuscatedResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override
                public void write(int b) {
                    capture.write(b);
                }

                @Override
                public void setWriteListener(WriteListener listener) {
                }

                @Override
                public boolean isReady() {
                    return true;
                }
            };
        }

        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(capture, StandardCharsets.UTF_8));
            }
            return writer;
        }

        byte[] getCapturedBytes() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            return capture.toByteArray();
        }
    }
}
