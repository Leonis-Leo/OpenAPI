package com.openapi.api.proxy;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易熔断器（按上游地址隔离，进程内状态）。
 *
 * <p>状态机：CLOSED（正常）→ 连续失败达到阈值 → OPEN（快速失败）→ 冷却后 → HALF_OPEN（放行一次试探）→ 成功回 CLOSED / 失败回 OPEN。</p>
 *
 * <p>注意：当前为进程内实现，多实例时各实例独立计数；生产级可替换为 Redis / Resilience4j。</p>
 */
@Component
public class CircuitBreaker {

    private final int failureThreshold;
    private final long openMillis;
    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    public CircuitBreaker() {
        this(5, 30_000L);
    }

    public CircuitBreaker(int failureThreshold, long openMillis) {
        this.failureThreshold = failureThreshold;
        this.openMillis = openMillis;
    }

    /** 是否放行请求。 */
    public boolean allowRequest(String key) {
        return states.computeIfAbsent(key, k -> new State()).allow();
    }

    public void recordSuccess(String key) {
        states.computeIfAbsent(key, k -> new State()).success();
    }

    public void recordFailure(String key) {
        states.computeIfAbsent(key, k -> new State()).failure();
    }

    private final class State {
        private static final int CLOSED = 0;
        private static final int OPEN = 1;
        private static final int HALF_OPEN = 2;

        private volatile int mode = CLOSED;
        private int consecutiveFailures = 0;
        private long openedAt = 0;

        synchronized boolean allow() {
            long now = System.currentTimeMillis();
            if (mode == OPEN) {
                if (now - openedAt >= openMillis) {
                    mode = HALF_OPEN;
                } else {
                    return false;
                }
            }
            return true;
        }

        synchronized void success() {
            consecutiveFailures = 0;
            mode = CLOSED;
        }

        synchronized void failure() {
            consecutiveFailures++;
            if (mode == HALF_OPEN || consecutiveFailures >= failureThreshold) {
                mode = OPEN;
                openedAt = System.currentTimeMillis();
            }
        }
    }
}
