package com.openapi.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class CsrfInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper;
    @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        if (!requiresCheck(req)) return true;
        String cookie = null; if (req.getCookies() != null) for (Cookie c : req.getCookies()) if ("openapi_csrf".equals(c.getName())) cookie = c.getValue();
        String header = req.getHeader("X-CSRF-Token");
        if (!StringUtils.hasText(cookie) || !StringUtils.hasText(header) || !MessageDigest.isEqual(cookie.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8))) {
            res.setStatus(403); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.CSRF_INVALID))); return false;
        }
        return true;
    }
    private boolean requiresCheck(HttpServletRequest r) { String m = r.getMethod(); return r.getRequestURI().startsWith("/v1/") && !("GET".equals(m) || "HEAD".equals(m) || "OPTIONS".equals(m) || r.getRequestURI().equals("/v1/user/login") || r.getRequestURI().equals("/v1/user/register")); }
}
