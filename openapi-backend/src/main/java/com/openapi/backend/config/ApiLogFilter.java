package com.openapi.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API 日志过滤器：缓存请求体与响应体，供签名拦截器采集出入参。
 */
@Component
public class ApiLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            HttpServletRequest wrappedRequest = request;
            if (isFormPost(request)) {
                request.setCharacterEncoding(StandardCharsets.UTF_8.name());
                wrappedRequest = new ContentCachingRequestWrapper(request);
            }
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(wrappedRequest, responseWrapper);
            responseWrapper.copyBodyToResponse();
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
