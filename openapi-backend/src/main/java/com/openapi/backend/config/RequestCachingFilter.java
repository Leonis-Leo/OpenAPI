package com.openapi.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 将表单 POST 请求包装为可重复读取的请求，供签名拦截器读取请求体参与签名计算。
 */
@Component
public class RequestCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isFormPost(request)) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            filterChain.doFilter(new ContentCachingRequestWrapper(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isFormPost(HttpServletRequest request) {
        String contentType = request.getContentType();
        return "POST".equalsIgnoreCase(request.getMethod())
                && contentType != null
                && contentType.toLowerCase().startsWith("application/x-www-form-urlencoded");
    }
}
