package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.common.PageResult;
import com.choosephd.dto.PageQuery;
import com.choosephd.dto.RankingEntryVo;
import com.choosephd.entity.RankingSource;
import com.choosephd.service.RankingSourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排名榜单 source + entry controller — 公开读 3 端点。
 *
 * <p>端点：
 * <ul>
 *   <li>GET / — 所有 active source 列表</li>
 *   <li>GET /{sourceId}/entries — 单榜单下分页查 entry</li>
 *   <li>GET /by-university/{universityId} — 单院校历年所有榜单 entry</li>
 * </ul>
 *
 * <p>无鉴权要求（数据公开）。Admin 写操作在 {@link com.choosephd.controller.v1.AdminImportController}。
 *
 * <p>Service：{@link com.choosephd.service.RankingSourceService}。
 */
@RestController
@RequestMapping("/api/v1/sources")
public class RankingSourceController {

    private final RankingSourceService rankingSourceService;

    public RankingSourceController(RankingSourceService rankingSourceService) {
        this.rankingSourceService = rankingSourceService;
    }

    @GetMapping
    public ApiResult<PageResult<RankingSource>> listSources(
            @RequestParam(required = false) Integer kind,
            @RequestParam(required = false) String ownerOrg,
            PageQuery query) {
        return ApiResult.ok(rankingSourceService.listSources(kind, ownerOrg, query));
    }

    @GetMapping("/{id}")
    public ApiResult<RankingSource> getSource(@PathVariable Integer id) {
        return ApiResult.ok(rankingSourceService.getSource(id));
    }

    @GetMapping("/{id}/entries")
    public ApiResult<PageResult<RankingEntryVo>> listSourceEntries(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "20") Long size) {
        return ApiResult.ok(rankingSourceService.listSourceEntries(id, year, page, size));
    }

    @GetMapping("/{id}/years")
    public ApiResult<List<Integer>> listSourceYears(@PathVariable Integer id) {
        return ApiResult.ok(rankingSourceService.listSourceYears(id));
    }
}
