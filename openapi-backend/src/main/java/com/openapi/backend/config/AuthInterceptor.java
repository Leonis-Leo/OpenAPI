package com.openapi.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.backend.common.JwtUtils;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录鉴权拦截器：校验 /v1/** 请求的 JWT，解析出当前用户。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return reject(response);
        }
        try {
            Long userId = jwtUtils.parseUserId(authorization.substring(7));
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
