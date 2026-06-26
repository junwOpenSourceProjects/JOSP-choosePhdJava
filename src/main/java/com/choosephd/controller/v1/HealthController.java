package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 健康检查 controller — 返回服务状态 + 当前时间。
 *
 * <p>用于：(1) K8s readiness/liveness probe；(2) 部署后冒烟测试。
 *
 * <p>无鉴权要求（任何 monitoring 工具都能调），无依赖外部服务（直接返内存时间）。
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ApiResult<Map<String, String>> health() {
        return ApiResult.ok(Map.of(
                "status", "up",
                "time", Instant.now().toString()
        ));
    }
}
