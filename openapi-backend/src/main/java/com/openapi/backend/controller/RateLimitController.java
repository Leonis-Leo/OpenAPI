package com.openapi.backend.controller;

import com.openapi.backend.entity.User;
import com.openapi.backend.service.RateLimitConfigService;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/ratelimit")
@RequiredArgsConstructor
@Tag(name = "限流配置")
public class RateLimitController {

    private final RateLimitConfigService rateLimitConfigService;
    private final UserService userService;

    @GetMapping("/list")
    @Operation(summary = "限流配置列表（管理员）")
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(rateLimitConfigService.listWithInterfaces());
    }

    @PostMapping("/save")
    @Operation(summary = "保存接口限流配置（管理员）")
    public ApiResponse<Void> save(
            @RequestParam Long interfaceId,
            @Parameter(example = "10") @RequestParam int capacity,
            @Parameter(example = "2") @RequestParam int refillRate,
            @RequestParam Boolean enabled,
            HttpServletRequest request) {
        requireAdmin(request);
        rateLimitConfigService.saveConfig(interfaceId, capacity, refillRate, enabled);
        return ApiResponse.ok();
    }

    @PostMapping("/delete")
    @Operation(summary = "删除接口限流配置（管理员）")
    public ApiResponse<Void> delete(@RequestParam Long interfaceId, HttpServletRequest request) {
        requireAdmin(request);
        rateLimitConfigService.deleteConfig(interfaceId);
        return ApiResponse.ok();
    }

    @GetMapping("/global")
    @Operation(summary = "全局限流配置（管理员）")
    public ApiResponse<Map<String, Object>> global(HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(rateLimitConfigService.getGlobalConfig());
    }

    @PostMapping("/global/save")
    @Operation(summary = "保存全局限流配置（管理员）")
    public ApiResponse<Void> saveGlobal(@RequestParam int capacity,
                                        @RequestParam int refillRate,
                                        HttpServletRequest request) {
        requireAdmin(request);
        rateLimitConfigService.saveGlobalConfig(capacity, refillRate);
        return ApiResponse.ok();
    }

    private void requireAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        User user = userService.getById(userId);
        if (user == null || !"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "仅管理员可操作");
        }
    }
}
