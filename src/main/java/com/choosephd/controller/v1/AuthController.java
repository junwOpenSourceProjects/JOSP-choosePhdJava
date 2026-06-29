package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.common.BusinessException;
import com.choosephd.dto.AuthRequest;
import com.choosephd.dto.AuthResponse;
import com.choosephd.dto.UserInfo;
import com.choosephd.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /** 同一 IP 每天最多注册数 */
    private static final int MAX_REGISTRATIONS_PER_IP_PER_DAY = 3;

    /** IP → (日期, 注册次数) */
    private final ConcurrentHashMap<String, IpRegistrationCounter> regCounters = new ConcurrentHashMap<>();

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResult<AuthResponse> register(@RequestBody AuthRequest request,
                                             HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        IpRegistrationCounter counter = regCounters.computeIfAbsent(ip, k -> new IpRegistrationCounter());

        int count = counter.incrementAndGet();
        if (count > MAX_REGISTRATIONS_PER_IP_PER_DAY) {
            throw new BusinessException(429, "该IP今日注册次数已达上限");
        }

        try {
            return ApiResult.ok(authService.register(request));
        } catch (Exception e) {
            // 注册失败回滚计数
            counter.decrement();
            throw e;
        }
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

    /** IP 级别注册计数器，按天重置。 */
    private static class IpRegistrationCounter {
        volatile LocalDate date = LocalDate.now();
        final AtomicInteger count = new AtomicInteger(0);

        int incrementAndGet() {
            resetIfNewDay();
            return count.incrementAndGet();
        }

        void decrement() {
            count.decrementAndGet();
        }

        private void resetIfNewDay() {
            LocalDate today = LocalDate.now();
            if (!today.equals(date)) {
                synchronized (this) {
                    if (!today.equals(date)) {
                        date = today;
                        count.set(0);
                    }
                }
            }
        }
    }
}
