package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.dto.UniversityTagVo;
import com.choosephd.service.UniversityTagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 院校标签公开读 controller — 单端点 GET / 返回所有 active 标签。
 *
 * <p>无鉴权要求（标签公开访问）。Admin 写操作在 {@link com.choosephd.controller.v1.UniversityTagAdminController}。
 *
 * <p>Service：{@link com.choosephd.service.UniversityTagService}。
 */
@RestController
@RequestMapping("/api/v1/university-tags")
public class UniversityTagController {

    private final UniversityTagService universityTagService;

    public UniversityTagController(UniversityTagService universityTagService) {
        this.universityTagService = universityTagService;
    }

    @GetMapping
    public ApiResult<List<UniversityTagVo>> listTags() {
        return ApiResult.ok(universityTagService.listActiveTags());
    }

    @GetMapping("/university/{universityId}")
    public ApiResult<List<UniversityTagVo>> listTagsByUniversity(@PathVariable String universityId) {
        return ApiResult.ok(universityTagService.listTagsByUniversity(universityId));
    }

    @PostMapping("/university/{universityId}/tags")
    public ApiResult<Void> setUniversityTags(@PathVariable String universityId,
                                              @RequestBody List<Integer> tagIds) {
        universityTagService.setUniversityTags(universityId, tagIds);
        return ApiResult.ok();
    }

    @PostMapping("/university/{universityId}/tags/{tagId}")
    public ApiResult<Void> assignTag(@PathVariable String universityId,
                                      @PathVariable Integer tagId) {
        universityTagService.assignTag(universityId, tagId);
        return ApiResult.ok();
    }

    @DeleteMapping("/university/{universityId}/tags/{tagId}")
    public ApiResult<Void> removeTag(@PathVariable String universityId,
                                      @PathVariable Integer tagId) {
        universityTagService.removeTag(universityId, tagId);
        return ApiResult.ok();
    }
}
