package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.entity.UserAccount;
import com.choosephd.repository.UserAccountMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin 会员管理端点 — 升级/降级用户 membership。
 * 路径在 /api/v1/admin/** → AdminInterceptor 兜底校验 ROLE_ADMIN。
 */
@RestController
@RequestMapping("/api/v1/admin/membership")
public class AdminMembershipController {

    private final UserAccountMapper userAccountMapper;

    public AdminMembershipController(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @PutMapping("/{username}")
    public ApiResult<Map<String, String>> setMembership(@PathVariable String username,
                                                         @RequestBody Map<String, String> body) {
        String tier = body.get("membership");
        if (tier == null || (!tier.equals("free") && !tier.equals("pro"))) {
            return ApiResult.error(400, "membership must be 'free' or 'pro'");
        }
        UserAccount user = userAccountMapper.selectByUsername(username);
        if (user == null) {
            return ApiResult.error(404, "用户不存在: " + username);
        }
        user.setMembership(tier);
        userAccountMapper.updateById(user);
        return ApiResult.ok(Map.of("username", username, "membership", tier));
    }
}
