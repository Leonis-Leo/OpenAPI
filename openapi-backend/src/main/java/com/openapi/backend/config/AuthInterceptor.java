package com.openapi.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.backend.common.JwtUtils;
import com.openapi.backend.entity.User;
import com.openapi.backend.service.UserService;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;

/**
 * 登录鉴权拦截器：校验 /v1/** 请求的 JWT，解析出当前用户。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String authorization = request.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        } else if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> "openapi_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        if (!StringUtils.hasText(token)) {
            return reject(response);
        }
        try {
            Long userId = jwtUtils.parseUserId(token);
            User user = userService.getById(userId);
            if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
                return reject(response);
            }
            request.setAttribute("openapi.userId", userId);
            return true;
        } catch (Exception e) {
            return reject(response);
        }
    }

    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.NO_AUTH)));
        return false;
    }
}
