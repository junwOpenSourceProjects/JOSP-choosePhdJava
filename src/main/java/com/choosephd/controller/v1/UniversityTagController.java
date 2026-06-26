package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.dto.UniversityTagVo;
import com.choosephd.service.UniversityTagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
