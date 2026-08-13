package com.openapi.gateway.filter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 令牌桶限流 Lua 脚本集成测试（需本机 Redis，CI 已随服务启动）。
 *
 * <p>通过显式传入「当前时间秒」ARGV 控制令牌补充，验证脚本的
 * 初始容量、扣减、按时间补充、容量封顶与拒绝行为。</p>
 */
class RateLimitLuaScriptTest {

    private static StringRedisTemplate redis;
    private static DefaultRedisScript<Long> script;
    private static boolean redisAvailable;

    @BeforeAll
    static void setUp() {
        script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        try {
            script.setScriptText(new String(
                    new ClassPathResource("rate_limit.lua").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8));
            LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
            factory.afterPropertiesSet();
            redis = new StringRedisTemplate(factory);
            redis.afterPropertiesSet();
            redis.getConnectionFactory().getConnection().ping();
            redisAvailable = true;
        } catch (Exception e) {
            redisAvailable = false;
        }
    }

    @AfterAll
    static void tearDown() {
        if (redisAvailable && redis.getConnectionFactory() instanceof LettuceConnectionFactory lettuce) {
            lettuce.destroy();
        }
    }

    private long acquire(String key, long capacity, long refillRate, long nowSeconds) {
        return redis.execute(script, List.of(key),
                String.valueOf(capacity), String.valueOf(refillRate), String.valueOf(nowSeconds), "1");
    }

    private void assumeRedis() {
        Assumptions.assumeTrue(redisAvailable, "Redis 不可达，跳过 Lua 集成测试");
    }

    @Test
    void firstRequestsAllowedAndConsumeTokens() {
        assumeRedis();
        String key = "openapi:ratelimit:test:" + UUID.randomUUID();
        assertEquals(1L, acquire(key, 5, 1, 1000));
        assertEquals(1L, acquire(key, 5, 1, 1000));
    }

    @Test
    void deniesWhenBucketEmpty() {
        assumeRedis();
        String key = "openapi:ratelimit:test:" + UUID.randomUUID();
        assertEquals(1L, acquire(key, 1, 0, 1000));
        assertEquals(0L, acquire(key, 1, 0, 1000));
    }

    @Test
    void refillsOverTime() {
        assumeRedis();
        String key = "openapi:ratelimit:test:" + UUID.randomUUID();
        // 容量 3，每秒补 1 个
        assertEquals(1L, acquire(key, 3, 1, 1000));
        assertEquals(1L, acquire(key, 3, 1, 1000));
        assertEquals(1L, acquire(key, 3, 1, 1000));
        assertEquals(0L, acquire(key, 3, 1, 1000)); // 桶空
        assertEquals(1L, acquire(key, 3, 1, 1002)); // 2 秒后补 2 个
        assertEquals(1L, acquire(key, 3, 1, 1002));
        assertEquals(0L, acquire(key, 3, 1, 1002));
    }

    @Test
    void capsAtCapacity() {
        assumeRedis();
        String key = "openapi:ratelimit:test:" + UUID.randomUUID();
        // 容量 2，补充速率很大，验证补充不超过容量
        assertEquals(1L, acquire(key, 2, 100, 1000));
        assertEquals(1L, acquire(key, 2, 100, 1000));
        assertEquals(0L, acquire(key, 2, 100, 1000)); // 桶空
        assertEquals(1L, acquire(key, 2, 100, 1100)); // 过了 100s，但只补到容量上限 2
        assertEquals(1L, acquire(key, 2, 100, 1100));
        assertEquals(0L, acquire(key, 2, 100, 1100));
    }
}
