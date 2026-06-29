package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.common.BusinessException;
import com.choosephd.dto.ShortlistItemVo;
import com.choosephd.dto.ShortlistRequest;
import com.choosephd.dto.UniversitySourceSummary;
import com.choosephd.entity.ExportLog;
import com.choosephd.repository.ExportLogMapper;
import com.choosephd.repository.RankingEntryMapper;
import com.choosephd.security.JwtService;
import com.choosephd.service.UserShortlistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shortlist")
public class UserShortlistController {

    private static final int PREMIUM_MONTHLY_EXPORT_LIMIT = 20;

    private final UserShortlistService userShortlistService;
    private final RankingEntryMapper rankingEntryMapper;
    private final ExportLogMapper exportLogMapper;
    private final JwtService jwtService;

    public UserShortlistController(UserShortlistService userShortlistService,
                                    RankingEntryMapper rankingEntryMapper,
                                    ExportLogMapper exportLogMapper,
                                    JwtService jwtService) {
        this.userShortlistService = userShortlistService;
        this.rankingEntryMapper = rankingEntryMapper;
        this.exportLogMapper = exportLogMapper;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ApiResult<List<ShortlistItemVo>> getShortlist(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.ok(userShortlistService.getShortlist(userId));
    }

    @PostMapping
    public ApiResult<ShortlistItemVo> upsertShortlist(@RequestBody ShortlistRequest req,
                                                      HttpServletRequest servletRequest) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        return ApiResult.ok(userShortlistService.upsertShortlist(userId, req));
    }

    @DeleteMapping("/{universityId}")
    public ApiResult<Void> deleteShortlist(@PathVariable String universityId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userShortlistService.deleteShortlist(userId, universityId);
        return ApiResult.ok();
    }

    /**
     * CSV 导出选校清单 — Premium 用户专属（每月 20 次）。
     * 导出列：院校中文名,院校英文名,国家,地区,QS排名,US News排名,THE排名,ARWU排名,CSRankings分数,优先级,备注
     */
    @GetMapping("/export")
    public void exportCsv(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            response.setStatus(401);
            return;
        }

        // Premium 权限检查
        String membership = extractMembership(request);
        if (!"premium".equals(membership)) {
            response.setStatus(402);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":402,\"message\":\"此功能需要高级版会员\"}");
            return;
        }

        // 本月导出次数检查
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        long thisMonth = exportLogMapper.countByUserIdSince(userId, monthStart);
        if (thisMonth >= PREMIUM_MONTHLY_EXPORT_LIMIT) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"本月导出次数已用完(" + PREMIUM_MONTHLY_EXPORT_LIMIT + "次/月)\"}");
            return;
        }

        List<ShortlistItemVo> items = userShortlistService.getShortlist(userId);
        if (items == null || items.isEmpty()) {
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"message\":\"选校清单为空\"}");
            return;
        }

        // 写 CSV
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"choosephd-shortlist-" + LocalDate.now() + ".csv\"");
        response.getOutputStream().write(0xEF); // BOM
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        PrintWriter w = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        w.println("院校中文名,院校英文名,国家,地区,QS排名,US News排名,THE排名,ARWU排名,CSRankings分数,优先级,备注");

        for (ShortlistItemVo item : items) {
            var uni = item.getUniversity();
            List<UniversitySourceSummary> sources = rankingEntryMapper.selectSourceSummariesByUniversity(item.getUniversityId());

            w.println(
                    csv(uni != null ? uni.getNameZh() : "") + "," +
                    csv(uni != null ? uni.getNameEn() : "") + "," +
                    csv(uni != null ? uni.getCountry() : "") + "," +
                    csv(uni != null ? uni.getRegion() : "") + "," +
                    findRank(sources, 4) + "," +   // QS
                    findRank(sources, 11) + "," +   // US News
                    findRank(sources, 16) + "," +   // THE
                    findRank(sources, 19) + "," +   // ARWU
                    findScore(sources, 46) + "," +   // CSRankings
                    (item.getPriority() != null ? item.getPriority() : "") + "," +
                    csv(item.getNote() != null ? item.getNote() : ""));
        }
        w.flush();

        // 记录导出日志
        ExportLog log = new ExportLog();
        log.setUserId(userId);
        log.setExportType("shortlist_csv");
        log.setCreatedAt(LocalDateTime.now());
        exportLogMapper.insert(log);
    }

    private String extractMembership(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return "free";
        try {
            String token = h.substring(7);
            if (!jwtService.validateToken(token)) return "free";
            Claims claims = jwtService.parseToken(token);
            String m = claims.get("membership", String.class);
            return m != null ? m : "free";
        } catch (Exception e) {
            return "free";
        }
    }

    private String findRank(List<UniversitySourceSummary> sources, int sourceId) {
        if (sources == null) return "";
        for (UniversitySourceSummary s : sources) {
            if (s.getSourceId() != null && s.getSourceId() == sourceId
                    && s.getLatestRankValue() != null && s.getLatestRankValue() > 0 && s.getLatestRankValue() < 9999) {
                return String.valueOf(s.getLatestRankValue());
            }
        }
        return "";
    }

    private String findScore(List<UniversitySourceSummary> sources, int sourceId) {
        if (sources == null) return "";
        for (UniversitySourceSummary s : sources) {
            if (s.getSourceId() != null && s.getSourceId() == sourceId
                    && s.getLatestRankDisplay() != null) {
                return s.getLatestRankDisplay();
            }
        }
        return "";
    }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
