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
 * 网关限流器：Redis + Lua 令牌桶。
 *
 * <p>优先读取管理平台配置的「按接口限流」配置（Redis 缓存，key 为 method:path），
 * 未配置或未启用时回退到全局按 AccessKey 限流。</p>
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

    @Value("${openapi.rate-limit.capacity:20}")
    private long capacity;

    @Value("${openapi.rate-limit.refill-rate:5}")
    private long refillRate;

    @Value("${openapi.rate-limit.key-prefix:openapi:ratelimit:}")
    private String keyPrefix;

    @Value("${openapi.rate-limit.config-prefix:openapi:ratelimit:config:}")
    private String configPrefix;

    /**
     * 按 AccessKey + 接口路径限流：优先使用接口级配置，否则使用全局配置。
     *
     * @return true 放行；false 触发限流
     */
    public Mono<Boolean> tryAcquire(String accessKey, String method, String path) {
        if (!enabled) {
            return Mono.just(true);
        }
        String configKey = configPrefix + method + ":" + path;
        return redisTemplate.opsForValue().get(configKey)
                .flatMap(json -> {
                    RateLimitConfigValue config = parseConfig(json);
                    if (config != null && config.enabled()) {
                        return tryAcquire(keyPrefix + "i:" + method + ":" + path,
                                config.capacity(), config.refillRate());
                    }
                    return tryAcquire(keyPrefix + accessKey, capacity, refillRate);
                })
                .switchIfEmpty(Mono.defer(() -> tryAcquire(keyPrefix + accessKey, capacity, refillRate)));
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
