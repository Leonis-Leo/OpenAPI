package com.openapi.backend.service;

import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginSecurityService {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final StringRedisTemplate redis;

    public void checkAllowed(String account) {
        try {
            if (Boolean.TRUE.equals(redis.hasKey(lockKey(account)))) throw new BusinessException(ErrorCode.RATE_LIMITED, "登录失败次数过多，请 15 分钟后重试");
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    }

    public void recordFailure(String account) {
        try {
            String key = failKey(account);
            Long count = redis.opsForValue().increment(key);
            redis.expire(key, WINDOW);
            if (count != null && count >= MAX_FAILURES) redis.opsForValue().set(lockKey(account), "1", WINDOW);
        } catch (Exception e) { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    }

    public void clear(String account) {
        try { redis.delete(java.util.List.of(failKey(account), lockKey(account))); }
        catch (Exception e) { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    }

    private String failKey(String account) { return "openapi:login:fail:" + account; }
    private String lockKey(String account) { return "openapi:login:lock:" + account; }
}
