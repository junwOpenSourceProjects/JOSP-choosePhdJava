package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.dto.ShortlistItemVo;
import com.choosephd.dto.ShortlistRequest;
import com.choosephd.service.UserShortlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户选校清单 controller — 登录用户专属 CRUD。
 *
 * <p>端点：
 * <ul>
 *   <li>GET / — 当前用户选校清单</li>
 *   <li>POST / — 加一项或改优先级 (upsert)</li>
 *   <li>DELETE /{itemId} — 删一项</li>
 *   <li>PUT /reorder — 批量改优先级</li>
 * </ul>
 *
 * <p>鉴权：必须登录（{@link com.choosephd.security.AuthInterceptor} 拦截）。
 * userId 从 {@code request.getAttribute("userId")} 取，避免前端传不可信 userId。
 *
 * <p>Service：{@link com.choosephd.service.UserShortlistService}。
 */
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
