package com.openapi.api.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitBreakerTest {

    @Test
    void allowsInitially() {
        CircuitBreaker cb = new CircuitBreaker();
        assertTrue(cb.allowRequest("up1"));
    }

    @Test
    void opensAfterConsecutiveFailures() {
        CircuitBreaker cb = new CircuitBreaker(2, 30_000L);
        cb.recordFailure("up1");
        assertTrue(cb.allowRequest("up1"));
        cb.recordFailure("up1");
        assertFalse(cb.allowRequest("up1"));
    }

    @Test
    void closesAfterSuccess() {
        CircuitBreaker cb = new CircuitBreaker(2, 30_000L);
        cb.recordFailure("up1");
        cb.recordFailure("up1");
        assertFalse(cb.allowRequest("up1"));
        cb.recordSuccess("up1");
        assertTrue(cb.allowRequest("up1"));
    }

    @Test
    void isolatesByUpstream() {
        CircuitBreaker cb = new CircuitBreaker(2, 30_000L);
        cb.recordFailure("up1");
        cb.recordFailure("up1");
        assertFalse(cb.allowRequest("up1"));
        assertTrue(cb.allowRequest("up2"));
    }

    @Test
    void halfOpenAllowsTrialAfterCooldown() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 50L);
        cb.recordFailure("up1");
        cb.recordFailure("up1");
        assertFalse(cb.allowRequest("up1"));
        Thread.sleep(70L);
        assertTrue(cb.allowRequest("up1")); // HALF_OPEN 放行一次试探
    }
}
