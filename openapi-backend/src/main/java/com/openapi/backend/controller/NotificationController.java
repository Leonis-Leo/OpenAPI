package com.openapi.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openapi.backend.entity.Notification;
import com.openapi.backend.service.NotificationService;
import com.openapi.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/notification")
@RequiredArgsConstructor
@Tag(name = "站内通知")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/page")
    @Operation(summary = "我的通知分页列表")
    public ApiResponse<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer read,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        Page<Notification> page = notificationService.pageByUser(userId, read, current, size);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        return ApiResponse.ok(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数量")
    public ApiResponse<Long> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        return ApiResponse.ok(notificationService.unreadCount(userId));
    }

    @PostMapping("/read")
    @Operation(summary = "标记单条通知已读")
    public ApiResponse<Void> read(@RequestParam Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        notificationService.markRead(userId, id);
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ApiResponse<Void> readAll(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        notificationService.markAllRead(userId);
        return ApiResponse.ok();
    }

    @PostMapping("/delete")
    @Operation(summary = "删除单条通知")
    public ApiResponse<Void> delete(@RequestParam Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        notificationService.delete(userId, id);
        return ApiResponse.ok();
    }

    @PostMapping("/clear")
    @Operation(summary = "清空我的全部通知")
    public ApiResponse<Void> clear(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        notificationService.clearAll(userId);
        return ApiResponse.ok();
    }
}
