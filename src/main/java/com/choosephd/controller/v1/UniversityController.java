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
        if (!isPaid) {
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
        if (!isPaid2) {
            maskRankingsForGuest(result.getList());
        }
        return ApiResult.ok(result);
    }

    /**
     * 未登录/免费用户 mask 详情页排名汇总。
     * Top 10 榜单保留数据（保障 GEO 索引），10 名之外 mask。
     */
    private void maskDetailForGuest(UniversityDetailResponse detail) {
        if (detail.getSources() != null) {
            for (UniversitySourceSummary s : detail.getSources()) {
                if (s.getLatestRankValue() == null || s.getLatestRankValue() <= 0
                        || s.getLatestRankValue() > 10) {
                    s.setLatestRankDisplay("🔒 月度解锁");
                    s.setLatestRankValue(-1);
                }
            }
        }
    }

    /**
     * 未登录/免费用户 mask 排行榜列表。
     * 前 10 名保留完整数据 + 按年精度；10 名之后 5 年精度降级 + mask。
     */
    private void maskRankingsForGuest(List<RankingEntryVo> list) {
        if (list == null) return;
        int pos = 0;
        for (RankingEntryVo entry : list) {
            pos++;
            if (pos <= 10) continue; // Top 10 完整可见
            // 10 名之后：5 年精度降级
            if (entry.getYear() != null && entry.getYear() % 5 != 0) {
                entry.setRankValue(-1);
                entry.setRankDisplay("🔒 月度解锁");
                entry.setScore(null);
                entry.setRankDelta(null);
            }
        }
    }
}
