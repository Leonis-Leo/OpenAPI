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
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
            @Parameter(description = "密码", example = "123456") @RequestParam String userPassword) {
        User user = userService.login(userAccount, userPassword);
        String token = jwtUtils.generateToken(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return ApiResponse.ok(result);
    }
}
