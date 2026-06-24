package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

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
