package com.openapi.backend.controller;

import com.openapi.backend.entity.InterfaceSubscribe;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.entity.User;
import com.openapi.backend.service.InterfaceInfoService;
import com.openapi.backend.service.InterfaceSubscribeService;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/interface")
@RequiredArgsConstructor
@Tag(name = "接口管理")
public class InterfaceInfoController {

    private final InterfaceInfoService interfaceInfoService;
    private final InterfaceSubscribeService subscribeService;
    private final UserService userService;

    @GetMapping("/list")
    @Operation(summary = "查询已上线接口")
    public ApiResponse<List<InterfaceInfo>> listOnline() {
        return ApiResponse.ok(interfaceInfoService.listOnline());
    }

    @GetMapping("/list-all")
    @Operation(summary = "全部接口列表（管理员）")
    public ApiResponse<List<InterfaceInfo>> listAll(HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(interfaceInfoService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询接口详情")
    public ApiResponse<InterfaceInfo> detail(
            @Parameter(description = "接口 ID", example = "1") @PathVariable Long id) {
        InterfaceInfo info = interfaceInfoService.getById(id);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        return ApiResponse.ok(info);
    }

    @PostMapping("/online")
    @Operation(summary = "接口上线（管理员）")
    public ApiResponse<Void> online(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        interfaceInfoService.online(id);
        return ApiResponse.ok();
    }

    @PostMapping("/offline")
    @Operation(summary = "接口下线（管理员）")
    public ApiResponse<Void> offline(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        interfaceInfoService.offline(id);
        return ApiResponse.ok();
    }

    @PostMapping("/subscribe")
    @Operation(summary = "订阅接口")
    public ApiResponse<InterfaceSubscribe> subscribe(@RequestParam Long interfaceId,
                                                     @RequestParam Long appId,
                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        return ApiResponse.ok(subscribeService.subscribe(userId, interfaceId, appId));
    }

    @GetMapping("/my-subscribes")
    @Operation(summary = "我的订阅列表")
    public ApiResponse<List<Map<String, Object>>> mySubscribes(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        return ApiResponse.ok(subscribeService.listByUser(userId));
    }

    @GetMapping("/subscribes")
    @Operation(summary = "订阅审批列表（管理员）")
    public ApiResponse<List<Map<String, Object>>> subscribes(
            @Parameter(description = "状态：0待审批 1通过 2拒绝", example = "0")
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(subscribeService.listByStatus(status));
    }

    @PostMapping("/approve")
    @Operation(summary = "订阅审批（管理员）")
    public ApiResponse<Void> approve(@RequestParam Long id,
                                     @Parameter(example = "true") @RequestParam Boolean approved,
                                     HttpServletRequest request) {
        requireAdmin(request);
        subscribeService.approve(id, approved);
        return ApiResponse.ok();
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "取消订阅")
    public ApiResponse<Void> unsubscribe(@RequestParam Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        subscribeService.unsubscribe(userId, id);
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
