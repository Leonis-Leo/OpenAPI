package com.openapi.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.api.proxy.UpstreamProxyService;
import com.openapi.domain.entity.InterfaceInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 上游代理兜底路由：匹配 /api/** 中除本地实现的接口（如 /api/demo/*）外的所有请求，
 * 若接口配置了上游地址则代理转发，否则返回 404。
 */
@RestController
@RequiredArgsConstructor
public class ProxyController {

    private final UpstreamProxyService upstreamProxy;
    private final ObjectMapper objectMapper;

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {
        InterfaceInfo info = (InterfaceInfo) request.getAttribute("openapi.interfaceInfo");
        if (info == null || !StringUtils.hasText(info.getUpstream())) {
            return notFound();
        }
        return upstreamProxy.forward(info, request);
    }

    private ResponseEntity<byte[]> notFound() {
        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of("code", 404, "message", "接口不存在或未配置上游服务"));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]);
        }
    }
}
