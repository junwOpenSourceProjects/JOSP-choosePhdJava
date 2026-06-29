package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.common.PageResult;
import com.choosephd.dto.PageQuery;
import com.choosephd.dto.RankingEntryVo;
import com.choosephd.dto.UniversityDetailResponse;
import com.choosephd.dto.UniversitySourceSummary;
import com.choosephd.dto.UniversityVo;
import com.choosephd.security.JwtService;
import com.choosephd.service.UniversityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/universities")
public class UniversityController {

    private final UniversityService universityService;
    private final JwtService jwtService;

    public UniversityController(UniversityService universityService, JwtService jwtService) {
        this.universityService = universityService;
        this.jwtService = jwtService;
    }

    /**
     * 判断当前请求是否携带有效 JWT（因为 /universities/* 在 AuthInterceptor 白名单中，
     * request attribute 不会被设置，这里手动检查 Authorization header）。
     */
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

    @GetMapping
    public ApiResult<PageResult<UniversityVo>> searchUniversities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String continent,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "bestRank") String sortBy,
            @RequestParam(required = false) List<Integer> tagIds,
            PageQuery query) {
        // 强制上限 30 条，防批量爬取
        if (query.getSize() > 30) {
            query.setSize(30L);
        }
        return ApiResult.ok(universityService.searchUniversities(keyword, continent, country, sortBy, tagIds, query));
    }

    @GetMapping("/countries")
    public ApiResult<List<String>> listCountries(@RequestParam(required = false) String continent) {
        return ApiResult.ok(universityService.listCountries(continent));
    }

    @GetMapping("/{urlId}")
    public ApiResult<UniversityDetailResponse> getUniversity(@PathVariable String urlId,
                                                              HttpServletRequest request) {
        UniversityDetailResponse detail = universityService.getUniversityDetail(urlId);
        if (!isAuthenticated(request)) {
            maskDetailForGuest(detail);
        }
        return ApiResult.ok(detail);
    }

    @GetMapping("/{urlId}/rankings")
    public ApiResult<PageResult<RankingEntryVo>> listUniversityRankings(
            @PathVariable String urlId,
            @RequestParam(required = false) Integer sourceId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean overallOnly,
            PageQuery query,
            HttpServletRequest request) {
        // 强制上限 50 条，防批量爬取
        if (query.getSize() > 50) {
            query.setSize(50L);
        }
        PageResult<RankingEntryVo> result = universityService.listUniversityRankings(urlId, sourceId, year, overallOnly, query);
        if (!isAuthenticated(request)) {
            maskRankingsForGuest(result.getList());
        }
        return ApiResult.ok(result);
    }

    private void maskDetailForGuest(UniversityDetailResponse detail) {
        if (detail.getSources() != null) {
            for (UniversitySourceSummary s : detail.getSources()) {
                s.setLatestRankDisplay("🔒 月度解锁");
                s.setLatestRankValue(-1);
            }
        }
    }

    private void maskRankingsForGuest(List<RankingEntryVo> list) {
        if (list == null) return;
        for (RankingEntryVo entry : list) {
            // 5年精度降级：非5的倍数年份 rank 置为 -1
            if (entry.getYear() != null && entry.getYear() % 5 != 0) {
                entry.setRankValue(-1);
                entry.setRankDisplay("🔒 月度解锁");
                entry.setScore(null);
                entry.setRankDelta(null);
            }
        }
    }
}
