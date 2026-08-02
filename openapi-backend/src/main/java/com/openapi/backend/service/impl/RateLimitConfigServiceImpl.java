package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.entity.RateLimitConfig;
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
    private static final String GLOBAL_KEY = "openapi:ratelimit:config:global";

    private final InterfaceInfoMapper interfaceInfoMapper;
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
            stringRedisTemplate.delete(configKey(info.getMethod(), info.getUrl()));
        }
    }

    private void syncToRedis(InterfaceInfo info, RateLimitConfig config) {
        String json = "{\"capacity\":" + config.getCapacity()
                + ",\"refillRate\":" + config.getRefillRate()
                + ",\"enabled\":" + (Integer.valueOf(1).equals(config.getEnabled()) ? "true" : "false") + "}";
        stringRedisTemplate.opsForValue().set(configKey(info.getMethod(), info.getUrl()), json);
    }

    private String configKey(String method, String url) {
        return REDIS_CONFIG_PREFIX + method + ":" + url;
    }

    @Override
    public Map<String, Object> getGlobalConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("capacity", 20);
        result.put("refillRate", 5);
        String json = stringRedisTemplate.opsForValue().get(GLOBAL_KEY);
        if (json != null) {
            try {
                JsonNode node = objectMapper.readTree(json);
                result.put("capacity", node.path("capacity").asInt(20));
                result.put("refillRate", node.path("refillRate").asInt(5));
            } catch (Exception ignored) {
                // 解析失败则用默认值
            }
        }
        return result;
    }

    @Override
    public void saveGlobalConfig(int capacity, int refillRate) {
        stringRedisTemplate.opsForValue().set(
                GLOBAL_KEY,
                "{\"capacity\":" + capacity + ",\"refillRate\":" + refillRate + "}");
    }
}
