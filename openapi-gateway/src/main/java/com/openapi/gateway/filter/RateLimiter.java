package com.openapi.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关限流器：Redis + Lua 令牌桶，两个维度——按应用（AccessKey）与按接口（method:path）。
 *
 * <p>每个维度独立读取管理平台配置（Redis 缓存），启用才限流，未配置则该维度不限流。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private record RateLimitConfigValue(long capacity, long refillRate, boolean enabled) {
    }

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> rateLimitScript = createScript();

    @Value("${openapi.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${openapi.rate-limit.key-prefix:openapi:ratelimit:}")
    private String keyPrefix;

    @Value("${openapi.rate-limit.config-prefix:openapi:ratelimit:config:}")
    private String configPrefix;

    /**
     * 双维度限流：按应用 + 按接口，任一维度拒绝即拒绝。
     *
     * @return true 放行；false 触发限流
     */
    public Mono<Boolean> tryAcquire(String accessKey, String path) {
        if (!enabled) {
            return Mono.just(true);
        }
        // 维度一：按应用（AccessKey）
        return tryAcquireWithConfig(
                        configPrefix + "app:" + accessKey,
                        keyPrefix + accessKey)
                .flatMap(appAllowed -> {
                    if (!appAllowed) {
                        return Mono.just(false);
                    }
                    // 维度二：按接口（path）
                    return tryAcquireWithConfig(
                            configPrefix + path,
                            keyPrefix + "i:" + path);
                });
    }

    /**
     * 读取某维度的限流配置并扣减令牌；未配置或未启用则放行。
     */
    private Mono<Boolean> tryAcquireWithConfig(String configKey, String bucketKey) {
        return redisTemplate.opsForValue().get(configKey)
                .flatMap(json -> {
                    RateLimitConfigValue config = parseConfig(json);
                    if (config != null && config.enabled()) {
                        return tryAcquire(bucketKey, config.capacity(), config.refillRate());
                    }
                    return Mono.just(true);
                })
                .switchIfEmpty(Mono.just(true));
    }

    private Mono<Boolean> tryAcquire(String key, long bucketCapacity, long bucketRefillRate) {
        List<String> keys = List.of(key);
        List<String> args = List.of(
                String.valueOf(bucketCapacity),
                String.valueOf(bucketRefillRate),
                String.valueOf(System.currentTimeMillis() / 1000),
                "1");
        return redisTemplate.execute(rateLimitScript, keys, args)
                .next()
                .map(result -> result != null && result == 1L)
                .onErrorResume(e -> {
                    log.warn("限流 Redis 异常，本次放行", e);
                    return Mono.just(true);
                });
    }

    private RateLimitConfigValue parseConfig(String json) {
        try {
            return objectMapper.readValue(json, RateLimitConfigValue.class);
        } catch (Exception e) {
            log.warn("限流配置解析失败: {}", json, e);
            return null;
        }
    }

    private static DefaultRedisScript<Long> createScript() {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setResultType(Long.class);
            script.setScriptText(new String(
                    new ClassPathResource("rate_limit.lua").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8));
            return script;
        } catch (IOException e) {
            throw new IllegalStateException("加载限流 Lua 脚本失败", e);
        }
    }
}
