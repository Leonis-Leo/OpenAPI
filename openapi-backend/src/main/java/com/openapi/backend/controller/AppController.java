package com.openapi.backend.controller;

import com.openapi.backend.entity.App;
import com.openapi.backend.service.AppService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import com.openapi.common.utils.KeyGeneratorUtils;
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

@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
@Tag(name = "应用管理")
public class AppController {

    private final AppService appService;

    @PostMapping("/create")
    @Operation(summary = "创建应用（生成密钥）")
    public ApiResponse<App> create(
            @Parameter(description = "应用名称", example = "我的应用") @RequestParam String appName,
            @Parameter(description = "用户 ID", example = "1") @RequestParam Long userId) {
        return ApiResponse.ok(appService.createApp(appName, userId));
    }

    @GetMapping("/list")
    @Operation(summary = "查询用户的应用列表")
    public ApiResponse<List<App>> list(
            @Parameter(description = "用户 ID", example = "1") @RequestParam Long userId) {
        return ApiResponse.ok(appService.listByUserId(userId));
    }

    @PostMapping("/update")
    @Operation(summary = "修改应用名称")
    public ApiResponse<Void> update(@RequestParam Long id,
                                    @RequestParam String appName,
                                    HttpServletRequest request) {
        App app = requireOwnedApp(id, request);
        app.setAppName(appName);
        appService.updateById(app);
        return ApiResponse.ok();
    }

    @PostMapping("/reset-secret")
    @Operation(summary = "重置 SecretKey")
    public ApiResponse<App> resetSecret(@RequestParam Long id, HttpServletRequest request) {
        App app = requireOwnedApp(id, request);
        app.setSecretKey("SK" + KeyGeneratorUtils.generateKey(32));
        appService.updateById(app);
        return ApiResponse.ok(app);
    }

    @PostMapping("/update-status")
    @Operation(summary = "启用/禁用应用")
    public ApiResponse<Void> updateStatus(@RequestParam Long id,
                                          @RequestParam Boolean enabled,
                                          HttpServletRequest request) {
        App app = requireOwnedApp(id, request);
        app.setStatus(enabled ? 1 : 0);
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
        Long userId = (Long) request.getAttribute("openapi.userId");
        App app = appService.getById(id);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用不存在或不属于当前用户");
        }
        return app;
    }
}
