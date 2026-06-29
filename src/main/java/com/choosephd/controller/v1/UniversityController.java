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
     * 判断当前请求是否携带有效 JWT。
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

    /**
     * 从 JWT 中读取 membership 字段。
     */
    private String getMembership(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return "free";
        try {
            String token = authHeader.substring(7);
            if (!jwtService.validateToken(token)) return "free";
            String m = jwtService.parseToken(token).get("membership", String.class);
            return m != null ? m : "free";
        } catch (Exception e) {
            return "free";
        }
    }

    @GetMapping
    public ApiResult<PageResult<UniversityVo>> searchUniversities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String continent,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "bestRank") String sortBy,
            @RequestParam(required = false) List<Integer> tagIds,
            PageQuery query,
            HttpServletRequest request) {
        // 免费用户上限 30，付费用户上限 100
        boolean isPaid = !"free".equals(getMembership(request));
        long maxSize = isPaid ? 100L : 30L;
        if (query.getSize() > maxSize) {
            query.setSize(maxSize);
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
        // 付费用户直接看完整数据；免费/未登录用户 mask
        boolean isPaid = !"free".equals(getMembership(request));
        if (!isPaid && !isAuthenticated(request)) {
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
        // 付费用户上限 200，免费用户上限 50
        boolean isPaid2 = !"free".equals(getMembership(request));
        long maxSize = isPaid2 ? 200L : 50L;
        if (query.getSize() > maxSize) {
            query.setSize(maxSize);
        }
        PageResult<RankingEntryVo> result = universityService.listUniversityRankings(urlId, sourceId, year, overallOnly, query);
        if (!isPaid2 && !isAuthenticated(request)) {
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
