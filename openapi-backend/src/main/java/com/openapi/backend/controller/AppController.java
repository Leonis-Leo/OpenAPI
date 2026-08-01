package com.openapi.backend.controller;

import com.openapi.backend.entity.App;
import com.openapi.backend.service.AppService;
import com.openapi.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
