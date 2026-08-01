package com.openapi.backend.controller;

import com.openapi.backend.entity.App;
import com.openapi.backend.service.AppService;
import com.openapi.common.model.ApiResponse;
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
public class AppController {

    private final AppService appService;

    @PostMapping("/create")
    public ApiResponse<App> create(@RequestParam String appName, @RequestParam Long userId) {
        return ApiResponse.ok(appService.createApp(appName, userId));
    }

    @GetMapping("/list")
    public ApiResponse<List<App>> list(@RequestParam Long userId) {
        return ApiResponse.ok(appService.listByUserId(userId));
    }
}
