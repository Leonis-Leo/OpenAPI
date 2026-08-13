package com.openapi.api.controller;

import com.openapi.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 已发布的示例接口（经网关 /api/** 路由，受签名鉴权保护）。
 */
@RestController
@RequestMapping("/api/demo")
@Tag(name = "演示接口")
public class DemoController {

    private static final String[] NAMES = {"Alice", "Bob", "Charlie", "David", "Emma"};

    @GetMapping("/name")
    @Operation(summary = "随机名称")
    public ApiResponse<String> randomName(
            @Parameter(description = "名称前缀", example = "Hi-") @RequestParam(required = false) String prefix) {
        String base = NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
        return ApiResponse.ok(prefix == null ? base : prefix + base);
    }

    @PostMapping("/echo")
    @Operation(summary = "参数回显")
    public ApiResponse<Map<String, String>> echo(
            @Parameter(description = "任意表单参数", example = "hello=world") @RequestParam Map<String, String> params) {
        return ApiResponse.ok(params);
    }
}
