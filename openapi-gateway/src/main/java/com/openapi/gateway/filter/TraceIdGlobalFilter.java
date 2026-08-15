package com.openapi.gateway.filter;

import com.openapi.common.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关链路追踪入口过滤器。
 *
 * <p>请求头已有合法 TraceId 则透传，否则生成；随后写入响应头并透传到数据面。</p>
 */
@Slf4j
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = TraceContext.resolve(request.getHeaders().getFirst(TraceContext.HEADER_TRACE_ID));

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TraceContext.HEADER_TRACE_ID, traceId)
                .build();
        exchange.getAttributes().put(TraceContext.ATTRIBUTE_KEY, traceId);
        exchange.getResponse().getHeaders().set(TraceContext.HEADER_TRACE_ID, traceId);

        TraceContext.put(traceId);
        try {
            log.info("gateway request traceId={} method={} path={}",
                    traceId, request.getMethod(), request.getPath());
        } finally {
            TraceContext.clear();
        }

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
