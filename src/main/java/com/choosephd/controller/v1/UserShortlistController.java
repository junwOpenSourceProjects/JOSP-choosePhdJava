package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.dto.ShortlistItemVo;
import com.choosephd.dto.ShortlistRequest;
import com.choosephd.service.UserShortlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shortlist")
public class UserShortlistController {

    private final UserShortlistService userShortlistService;

    public UserShortlistController(UserShortlistService userShortlistService) {
        this.userShortlistService = userShortlistService;
    }

    @GetMapping
    public ApiResult<List<ShortlistItemVo>> getShortlist(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.ok(userShortlistService.getShortlist(userId));
    }

    @PostMapping
    public ApiResult<ShortlistItemVo> upsertShortlist(@RequestBody ShortlistRequest request,
                                                      HttpServletRequest servletRequest) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        return ApiResult.ok(userShortlistService.upsertShortlist(userId, request));
    }

    @DeleteMapping("/{universityId}")
    public ApiResult<Void> deleteShortlist(@PathVariable String universityId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userShortlistService.deleteShortlist(userId, universityId);
        return ApiResult.ok();
    }
}
