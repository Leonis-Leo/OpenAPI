package com.openapi.backend.controller;

import com.openapi.backend.common.JwtUtils;
import com.openapi.backend.entity.User;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResponse<User> register(
            @Parameter(description = "账号", example = "admin") @RequestParam String userAccount,
            @Parameter(description = "密码", example = "123456") @RequestParam String userPassword,
            @Parameter(description = "昵称", example = "管理员") @RequestParam(required = false) String userName) {
        if (!StringUtils.hasText(userAccount) || !StringUtils.hasText(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        return ApiResponse.ok(userService.register(userAccount, userPassword, userName));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResponse<Map<String, Object>> login(
            @Parameter(description = "账号", example = "admin") @RequestParam String userAccount,
            @Parameter(description = "密码", example = "123456") @RequestParam String userPassword,
            HttpServletResponse response) {
        User user = userService.login(userAccount, userPassword);
        String token = jwtUtils.generateToken(user.getId());
        Cookie cookie = new Cookie("openapi_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (24 * 60 * 60));
        response.addCookie(cookie);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return ApiResponse.ok(result);
    }

    @GetMapping("/list")
    @Operation(summary = "用户列表（管理员）")
    public ApiResponse<List<User>> listUsers(
            @Parameter(description = "搜索关键字（账号/昵称）") @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(userService.listUsers(keyword));
    }

    @PostMapping("/update-role")
    @Operation(summary = "修改用户角色（管理员）")
    public ApiResponse<Void> updateRole(@RequestParam Long id,
                                        @Parameter(example = "admin") @RequestParam String role,
                                        HttpServletRequest request) {
        requireAdmin(request);
        userService.updateRole(id, role);
        return ApiResponse.ok();
    }

    @PostMapping("/update-status")
    @Operation(summary = "启用/禁用用户（管理员）")
    public ApiResponse<Void> updateStatus(@RequestParam Long id,
                                          @RequestParam Boolean enabled,
                                          HttpServletRequest request) {
        requireAdmin(request);
        userService.updateStatus(id, enabled ? 1 : 0);
        return ApiResponse.ok();
    }

    @PostMapping("/create")
    @Operation(summary = "新增用户（管理员）")
    public ApiResponse<User> create(@RequestParam String userAccount,
                                    @RequestParam String userPassword,
                                    @RequestParam(required = false) String userName,
                                    @Parameter(example = "user") @RequestParam(defaultValue = "user") String role,
                                    HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(userService.createUser(userAccount, userPassword, userName, role));
    }

    @PostMapping("/update")
    @Operation(summary = "编辑用户（昵称/重置密码，管理员）")
    public ApiResponse<Void> update(@RequestParam Long id,
                                    @RequestParam(required = false) String userName,
                                    @RequestParam(required = false) String userPassword,
                                    HttpServletRequest request) {
        requireAdmin(request);
        userService.updateUser(id, userName, userPassword);
        return ApiResponse.ok();
    }

    @PostMapping("/delete")
    @Operation(summary = "删除用户（管理员）")
    public ApiResponse<Void> delete(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        Long selfId = (Long) request.getAttribute("openapi.userId");
        if (selfId != null && selfId.equals(id)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "不能删除当前账号");
        }
        userService.deleteUser(id);
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
