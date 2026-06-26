package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.common.BusinessException;
import com.choosephd.dto.UniversityTagRequest;
import com.choosephd.entity.UniversityTag;
import com.choosephd.service.UniversityTagService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/university-tags")
public class UniversityTagAdminController {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final UniversityTagService universityTagService;

    public UniversityTagAdminController(UniversityTagService universityTagService) {
        this.universityTagService = universityTagService;
    }

    private void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (!ADMIN_ROLE.equals(role)) {
            throw new BusinessException("无权限");
        }
    }

    @GetMapping
    public ApiResult<List<UniversityTag>> listTags(HttpServletRequest request) {
        requireAdmin(request);
        return ApiResult.ok(universityTagService.listAllTags());
    }

    @PostMapping
    public ApiResult<UniversityTag> createTag(@RequestBody UniversityTagRequest request,
                                              HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return ApiResult.ok(universityTagService.createTag(request));
    }

    @PutMapping("/{id}")
    public ApiResult<UniversityTag> updateTag(@PathVariable Integer id,
                                              @RequestBody UniversityTagRequest request,
                                              HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return ApiResult.ok(universityTagService.updateTag(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteTag(@PathVariable Integer id,
                                     HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        universityTagService.deleteTag(id);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/universities")
    public ApiResult<Set<String>> listTagUniversities(@PathVariable Integer id,
                                                      HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        return ApiResult.ok(universityTagService.listUniversityIdsByTag(id));
    }

    @PutMapping("/universities/{urlId}")
    public ApiResult<Void> setUniversityTags(@PathVariable String urlId,
                                             @RequestBody List<Integer> tagIds,
                                             HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        universityTagService.setUniversityTags(urlId, tagIds);
        return ApiResult.ok();
    }
}
