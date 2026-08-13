package com.openapi.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openapi.backend.dto.AppResponse;
import com.openapi.domain.entity.App;
import com.openapi.domain.entity.User;
import com.openapi.backend.service.AppService;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import com.openapi.common.utils.KeyGeneratorUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
@Tag(name = "应用管理")
public class AppController {

    private final AppService appService;
    private final UserService userService;

    @PostMapping("/create")
    @Operation(summary = "创建应用（生成密钥）")
    public ApiResponse<AppResponse> create(@RequestParam String appName, HttpServletRequest request) {
        if (!StringUtils.hasText(appName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        }
        App app = appService.createApp(appName.trim(), currentUserId(request));
        return ApiResponse.ok(AppResponse.from(app, true));
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前用户的应用列表")
    public ApiResponse<List<AppResponse>> list(HttpServletRequest request) {
        return ApiResponse.ok(appService.listByUserId(currentUserId(request)).stream()
                .map(app -> AppResponse.from(app, false))
                .toList());
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询当前用户应用")
    public ApiResponse<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        LambdaQueryWrapper<App> wrapper = new LambdaQueryWrapper<App>()
                .eq(App::getUserId, currentUserId(request));
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(App::getAppName, keyword).or().like(App::getAccessKey, keyword));
        }
        wrapper.eq(status != null, App::getStatus, status).orderByDesc(App::getId);
        Page<App> page = appService.page(new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 100)), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(app -> AppResponse.from(app, false)).toList());
        result.put("total", page.getTotal());
        return ApiResponse.ok(result);
    }

    @GetMapping("/debug-list")
    @Operation(summary = "查询当前用户的调试应用列表")
    public ApiResponse<List<AppResponse>> debugList(HttpServletRequest request) {
        return ApiResponse.ok(appService.listByUserId(currentUserId(request)).stream()
                .map(app -> AppResponse.from(app, true))
                .toList());
    }

    @GetMapping("/admin-list")
    @Operation(summary = "管理员查询指定用户的应用列表")
    public ApiResponse<List<AppResponse>> adminList(@RequestParam Long userId, HttpServletRequest request) {
        User currentUser = userService.getById(currentUserId(request));
        if (currentUser == null || !"admin".equals(currentUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "仅管理员可操作");
        }
        return ApiResponse.ok(appService.listByUserId(userId).stream()
                .map(app -> AppResponse.from(app, false))
                .toList());
    }

    @PostMapping("/reveal-secret")
    @Operation(summary = "查看应用 SecretKey")
    public ApiResponse<AppResponse> revealSecret(@RequestParam Long id, HttpServletRequest request) {
        return ApiResponse.ok(AppResponse.from(requireOwnedApp(id, request), true));
    }

    @PostMapping("/update")
    @Operation(summary = "修改应用名称")
    public ApiResponse<Void> update(@RequestParam Long id,
                                    @RequestParam String appName,
                                    HttpServletRequest request) {
        if (!StringUtils.hasText(appName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        }
        App app = requireOwnedApp(id, request);
        app.setAppName(appName.trim());
        appService.updateById(app);
        return ApiResponse.ok();
    }

    @PostMapping("/reset-secret")
    @Operation(summary = "重置 SecretKey")
    public ApiResponse<AppResponse> resetSecret(@RequestParam Long id, @RequestParam String currentPassword, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        if (!userService.verifyPassword(userId, currentPassword)) throw new BusinessException(ErrorCode.NO_AUTH, "当前密码验证失败");
        App app = requireOwnedApp(id, request);
        app.setSecretKey("SK" + KeyGeneratorUtils.generateKey(32));
        appService.updateById(app);
        return ApiResponse.ok(AppResponse.from(app, true));
    }

    @PostMapping("/update-status")
    @Operation(summary = "启用/禁用应用")
    public ApiResponse<Void> updateStatus(@RequestParam Long id,
                                          @RequestParam Boolean enabled,
                                          HttpServletRequest request) {
        App app = requireOwnedApp(id, request);
        app.setStatus(Boolean.TRUE.equals(enabled) ? 1 : 0);
        appService.updateById(app);
        return ApiResponse.ok();
    }

    @PostMapping("/delete")
    @Operation(summary = "删除应用")
    public ApiResponse<Void> delete(@RequestParam Long id, HttpServletRequest request) {
        App app = requireOwnedApp(id, request);
        appService.removeById(app.getId());
        return ApiResponse.ok();
    }

    private App requireOwnedApp(Long id, HttpServletRequest request) {
        App app = appService.getById(id);
        if (app == null || !Objects.equals(app.getUserId(), currentUserId(request))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用不存在或不属于当前用户");
        }
        return app;
    }

    private Long currentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "请先登录");
        }
        return userId;
    }
}
