package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.backend.entity.App;
import com.openapi.backend.entity.AppRateLimitConfig;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.entity.RateLimitConfig;
import com.openapi.backend.mapper.AppMapper;
import com.openapi.backend.mapper.AppRateLimitConfigMapper;
import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.mapper.RateLimitConfigMapper;
import com.openapi.backend.service.RateLimitConfigService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RateLimitConfigServiceImpl extends ServiceImpl<RateLimitConfigMapper, RateLimitConfig>
        implements RateLimitConfigService {

    private static final String REDIS_CONFIG_PREFIX = "openapi:ratelimit:config:";
    private static final String REDIS_APP_PREFIX = "openapi:ratelimit:config:app:";

    private final InterfaceInfoMapper interfaceInfoMapper;
    private final AppMapper appMapper;
    private final AppRateLimitConfigMapper appRateLimitConfigMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> listWithInterfaces() {
        return interfaceInfoMapper.selectList(null).stream().map(info -> {
            Map<String, Object> map = new HashMap<>();
            map.put("interfaceId", info.getId());
            map.put("interfaceName", info.getName());
            map.put("url", info.getUrl());
            map.put("method", info.getMethod());
            RateLimitConfig config = lambdaQuery()
                    .eq(RateLimitConfig::getInterfaceId, info.getId())
                    .one();
            map.put("configured", config != null);
            map.put("capacity", config == null ? 20 : config.getCapacity());
            map.put("refillRate", config == null ? 5 : config.getRefillRate());
            map.put("enabled", config != null && Integer.valueOf(1).equals(config.getEnabled()));
            return map;
        }).toList();
    }

    @Override
    public void saveConfig(Long interfaceId, int capacity, int refillRate, boolean enabled) {
        InterfaceInfo info = interfaceInfoMapper.selectById(interfaceId);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        RateLimitConfig config = lambdaQuery()
                .eq(RateLimitConfig::getInterfaceId, interfaceId)
                .one();
        if (config == null) {
            config = new RateLimitConfig();
            config.setInterfaceId(interfaceId);
            config.setIsDelete(0);
        }
        config.setCapacity(capacity);
        config.setRefillRate(refillRate);
        config.setEnabled(enabled ? 1 : 0);
        saveOrUpdate(config);
        syncToRedis(info, config);
    }

    @Override
    public void deleteConfig(Long interfaceId) {
        baseMapper.physicalDeleteByInterface(interfaceId);
        InterfaceInfo info = interfaceInfoMapper.selectById(interfaceId);
        if (info != null) {
            stringRedisTemplate.delete(configKey(info.getUrl()));
        }
    }

    private void syncToRedis(InterfaceInfo info, RateLimitConfig config) {
        String json = "{\"capacity\":" + config.getCapacity()
                + ",\"refillRate\":" + config.getRefillRate()
                + ",\"enabled\":" + (Integer.valueOf(1).equals(config.getEnabled()) ? "true" : "false") + "}";
        stringRedisTemplate.opsForValue().set(configKey(info.getUrl()), json);
    }

    private String configKey(String url) {
        return REDIS_CONFIG_PREFIX + url;
    }

    @Override
    public List<Map<String, Object>> listAppsWithConfig() {
        return appMapper.selectList(null).stream().map(app -> {
            Map<String, Object> map = new HashMap<>();
            map.put("appId", app.getId());
            map.put("appName", app.getAppName());
            map.put("accessKey", app.getAccessKey());
            AppRateLimitConfig config = appRateLimitConfigMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppRateLimitConfig>()
                            .eq(AppRateLimitConfig::getAppId, app.getId()));
            map.put("configured", config != null);
            map.put("capacity", config == null ? 20 : config.getCapacity());
            map.put("refillRate", config == null ? 5 : config.getRefillRate());
            map.put("enabled", config != null && Integer.valueOf(1).equals(config.getEnabled()));
            return map;
        }).toList();
    }

    @Override
    public void saveAppConfig(Long appId, int capacity, int refillRate, boolean enabled) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用不存在");
        }
        AppRateLimitConfig config = appRateLimitConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppRateLimitConfig>()
                        .eq(AppRateLimitConfig::getAppId, appId));
        if (config == null) {
            config = new AppRateLimitConfig();
            config.setAppId(appId);
            config.setIsDelete(0);
        }
        config.setCapacity(capacity);
        config.setRefillRate(refillRate);
        config.setEnabled(enabled ? 1 : 0);
        if (config.getId() == null) {
            appRateLimitConfigMapper.insert(config);
        } else {
            appRateLimitConfigMapper.updateById(config);
        }
        stringRedisTemplate.opsForValue().set(
                REDIS_APP_PREFIX + app.getAccessKey(),
                "{\"capacity\":" + capacity + ",\"refillRate\":" + refillRate
                        + ",\"enabled\":" + (enabled ? "true" : "false") + "}");
    }

    @Override
    public void deleteAppConfig(Long appId) {
        App app = appMapper.selectById(appId);
        appRateLimitConfigMapper.physicalDeleteByApp(appId);
        if (app != null) {
            stringRedisTemplate.delete(REDIS_APP_PREFIX + app.getAccessKey());
        }
    }
}
