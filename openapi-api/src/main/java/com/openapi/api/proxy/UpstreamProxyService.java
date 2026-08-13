package com.openapi.api.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.domain.entity.InterfaceInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * 上游代理：按接口配置把 /api/** 请求转发到上游服务，带超时、重试与熔断。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpstreamProxyService {

    private static final Set<String> SKIP_HEADERS = Set.of(
            "host", "connection", "content-length", "transfer-encoding", "accept-encoding",
            "x-access-key", "x-timestamp", "x-nonce", "x-signature");

    private final CircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;

    public ResponseEntity<byte[]> forward(InterfaceInfo info, HttpServletRequest request) {
        String upstream = info.getUpstream();
        if (!circuitBreaker.allowRequest(upstream)) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "上游服务熔断中，请稍后重试");
        }
        int timeout = info.getTimeoutMs() == null ? 3000 : info.getTimeoutMs();
        int retries = info.getRetryCount() == null ? 0 : info.getRetryCount();
        String target = buildTargetUrl(upstream, request);
        HttpMethod method = HttpMethod.valueOf(request.getMethod().toUpperCase());
        HttpHeaders headers = copyHeaders(request);
        byte[] body = readBody(request);

        for (int attempt = 0; attempt <= retries; attempt++) {
            try {
                ResponseEntity<byte[]> resp = exchange(target, method, headers, body, timeout);
                circuitBreaker.recordSuccess(upstream);
                return resp;
            } catch (HttpClientErrorException e) {
                // 4xx 客户端错误，重试无意义，直接透传上游响应
                circuitBreaker.recordFailure(upstream);
                return error(HttpStatus.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());
            } catch (Exception e) {
                log.warn("上游调用失败 (attempt {}/{}): {}", attempt + 1, retries + 1, e.getMessage());
                if (attempt < retries) {
                    backoff(attempt);
                }
            }
        }
        circuitBreaker.recordFailure(upstream);
        return error(HttpStatus.BAD_GATEWAY, "上游服务调用失败");
    }

    private ResponseEntity<byte[]> exchange(String url, HttpMethod method, HttpHeaders headers, byte[] body, int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        RestTemplate restTemplate = new RestTemplate(factory);
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, method, entity, byte[].class);
    }

    private String buildTargetUrl(String upstream, HttpServletRequest request) {
        String base = upstream.endsWith("/") ? upstream.substring(0, upstream.length() - 1) : upstream;
        String query = request.getQueryString();
        return base + request.getRequestURI() + (query != null && !query.isEmpty() ? "?" + query : "");
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if (SKIP_HEADERS.contains(name.toLowerCase())) {
                continue;
            }
            headers.add(name, request.getHeader(name));
        }
        return headers;
    }

    private byte[] readBody(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return new byte[0];
        }
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.warn("读取请求体失败: {}", e.getMessage());
            return new byte[0];
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(Math.min(200L * (attempt + 1), 1000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ResponseEntity<byte[]> error(HttpStatus status, String message) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of("code", status.value(), "message", message));
            return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
        } catch (Exception e) {
            return ResponseEntity.status(status).body(new byte[0]);
        }
    }
}
