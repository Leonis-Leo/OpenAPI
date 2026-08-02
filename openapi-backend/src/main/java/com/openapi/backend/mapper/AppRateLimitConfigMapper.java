package com.openapi.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openapi.backend.entity.AppRateLimitConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface AppRateLimitConfigMapper extends BaseMapper<AppRateLimitConfig> {

    @Delete("DELETE FROM app_rate_limit_config WHERE app_id = #{appId}")
    int physicalDeleteByApp(@Param("appId") Long appId);
}
