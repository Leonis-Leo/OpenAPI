package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.domain.entity.RateLimitConfig;

import java.util.List;
import java.util.Map;

public interface RateLimitConfigService extends IService<RateLimitConfig> {

    List<Map<String, Object>> listWithInterfaces();

    void saveConfig(Long interfaceId, int capacity, int refillRate, boolean enabled);

    void deleteConfig(Long interfaceId);

    List<Map<String, Object>> listAppsWithConfig();

    void saveAppConfig(Long appId, int capacity, int refillRate, boolean enabled);

    void deleteAppConfig(Long appId);
}
