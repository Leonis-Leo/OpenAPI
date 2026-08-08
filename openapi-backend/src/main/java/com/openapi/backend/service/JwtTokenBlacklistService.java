package com.openapi.backend.service;

import com.openapi.backend.common.JwtUtils;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtTokenBlacklistService {
    private final StringRedisTemplate redis;
    private final JwtUtils jwtUtils;
    public boolean isBlacklisted(String token) {
        try { return Boolean.TRUE.equals(redis.hasKey(key(token))); }
        catch (Exception e) { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    }
    public void blacklist(String token) {
        try { long ttl = jwtUtils.remainingMillis(token); if (ttl > 0) redis.opsForValue().set(key(token), "1", Duration.ofMillis(ttl)); }
        catch (Exception e) { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
    }
    private String key(String token) { try { byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)); StringBuilder s = new StringBuilder(); for (byte b : d) s.append(String.format("%02x", b)); return "openapi:jwt:blacklist:" + s; } catch (Exception e) { throw new IllegalStateException(e); } }
}
