package com.openapi.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪上下文工具。
 *
 * <p>统一维护 HTTP 请求头、RabbitMQ 消息头与日志 MDC 中的 TraceId。
 * TraceId 采用 32 位小写十六进制（UUID 去除连字符）。</p>
 */
public final class TraceContext {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";
    public static final String ATTRIBUTE_KEY = "openapi.traceId";

    private static final String TRACE_ID_PATTERN = "[0-9a-fA-F]{32}";

    private TraceContext() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    public static boolean isValid(String traceId) {
        return traceId != null && traceId.matches(TRACE_ID_PATTERN);
    }

    /**
     * 若候选值合法则透传并统一为小写，否则生成新的 TraceId。
     */
    public static String resolve(String candidate) {
        return isValid(candidate) ? candidate.toLowerCase() : generate();
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }

    public static void put(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            MDC.remove(MDC_KEY);
        } else {
            MDC.put(MDC_KEY, traceId);
        }
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
