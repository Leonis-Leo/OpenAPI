package com.openapi.backend.controller;

import com.openapi.common.model.ApiResponse;
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
public class DemoController {

    private static final String[] NAMES = {"Alice", "Bob", "Charlie", "David", "Emma"};

    @GetMapping("/name")
    public ApiResponse<String> randomName(@RequestParam(required = false) String prefix) {
        String base = NAMES[ThreadLocalRandom.current().nextInt(NAMES.length)];
        return ApiResponse.ok(prefix == null ? base : prefix + base);
    }

    @PostMapping("/echo")
    public ApiResponse<Map<String, String>> echo(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(params);
    }
}
