package com.openapi.backend.controller;

import com.openapi.backend.entity.User;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestParam String userAccount,
                                      @RequestParam String userPassword,
                                      @RequestParam(required = false) String userName) {
        if (!StringUtils.hasText(userAccount) || !StringUtils.hasText(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        return ApiResponse.ok(userService.register(userAccount, userPassword, userName));
    }

    @PostMapping("/login")
    public ApiResponse<User> login(@RequestParam String userAccount, @RequestParam String userPassword) {
        return ApiResponse.ok(userService.login(userAccount, userPassword));
    }
}
