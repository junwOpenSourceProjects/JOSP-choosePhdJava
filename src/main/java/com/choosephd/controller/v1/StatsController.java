package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.dto.StatsOverviewResponse;
import com.choosephd.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard 统计 controller — 单端点 /overview 返回首页 4 大数字。
 *
 * <p>无鉴权要求（首页 Dashboard 公开访问）。
 *
 * <p>Service：{@link com.choosephd.service.StatsService}。
 */
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public ApiResult<StatsOverviewResponse> overview() {
        return ApiResult.ok(statsService.overview());
    }
}
