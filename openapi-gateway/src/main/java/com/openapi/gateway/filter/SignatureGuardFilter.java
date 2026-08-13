package com.openapi.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.common.constant.SignConstant;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import com.openapi.common.utils.SignatureHeaderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关统一入口过滤器：校验签名请求头与时间戳。
 *
 * <p>签名本身的校验在后端拦截器完成（后端可访问密钥库）；
 * 后续可在本过滤器补充 Redis + Lua 限流与 nonce 防重放。</p>
 */
@Component
@RequiredArgsConstructor
public class SignatureGuardFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    @Value("${openapi.sign.max-clock-skew-millis:300000}")
    private long maxClockSkewMillis;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String accessKey = request.getHeaders().getFirst(SignConstant.HEADER_ACCESS_KEY);
        String timestamp = request.getHeaders().getFirst(SignConstant.HEADER_TIMESTAMP);
        String nonce = request.getHeaders().getFirst(SignConstant.HEADER_NONCE);
        String signature = request.getHeaders().getFirst(SignConstant.HEADER_SIGNATURE);

        ErrorCode headerError = SignatureHeaderValidator.validate(
                accessKey, timestamp, nonce, signature, maxClockSkewMillis);
        if (headerError != null) {
            return writeJson(exchange, headerError);
        }

        // Redis + Lua 令牌桶限流（按接口配置，未配置时按 AccessKey）
        return rateLimiter.tryAcquire(accessKey, request.getURI().getPath())
                .flatMap(allowed -> {
                    if (!allowed) {
                        return writeJson(exchange, ErrorCode.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS);
                    }
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, ErrorCode errorCode) {
        return writeJson(exchange, errorCode, HttpStatus.UNAUTHORIZED);
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, ErrorCode errorCode, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(ApiResponse.error(errorCode));
        } catch (Exception e) {
            bytes = "{\"code\":50000,\"message\":\"系统内部错误\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
