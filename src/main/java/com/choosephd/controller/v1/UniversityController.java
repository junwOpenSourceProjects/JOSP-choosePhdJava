package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.common.PageResult;
import com.choosephd.dto.PageQuery;
import com.choosephd.dto.RankingEntryVo;
import com.choosephd.dto.UniversityDetailResponse;
import com.choosephd.dto.UniversityVo;
import com.choosephd.service.UniversityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 院校 controller — 公开读 3 端点 + 院校标签附加。
 *
 * <p>端点：
 * <ul>
 *   <li>GET / — 院校列表分页（关键字/国家/地区/标签筛选 + 排序）</li>
 *   <li>GET /countries — 不重复的国家列表（筛选用）</li>
 *   <li>GET /{urlId} — 单院校详情（基本信息 + 标签 + 历年榜单汇总）</li>
 * </ul>
 *
 * <p>无鉴权要求（数据公开）。
 *
 * <p>Service：{@link com.choosephd.service.UniversityService} +
 * {@link com.choosephd.service.UniversityTagService}（标签批量查询）。
 */
@RestController
@RequestMapping("/api/v1/universities")
public class UniversityController {

    private final UniversityService universityService;

    public UniversityController(UniversityService universityService) {
        this.universityService = universityService;
    }

    @GetMapping
    public ApiResult<PageResult<UniversityVo>> searchUniversities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String continent,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "bestRank") String sortBy,
            @RequestParam(required = false) List<Integer> tagIds,
            PageQuery query) {
        return ApiResult.ok(universityService.searchUniversities(keyword, continent, country, sortBy, tagIds, query));
    }

    @GetMapping("/countries")
    public ApiResult<List<String>> listCountries(@RequestParam(required = false) String continent) {
        return ApiResult.ok(universityService.listCountries(continent));
    }

    @GetMapping("/{urlId}")
    public ApiResult<UniversityDetailResponse> getUniversity(@PathVariable String urlId) {
        return ApiResult.ok(universityService.getUniversityDetail(urlId));
    }

    @GetMapping("/{urlId}/rankings")
    public ApiResult<PageResult<RankingEntryVo>> listUniversityRankings(
            @PathVariable String urlId,
            @RequestParam(required = false) Integer sourceId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean overallOnly,
            PageQuery query) {
        return ApiResult.ok(universityService.listUniversityRankings(urlId, sourceId, year, overallOnly, query));
    }
}
