package com.openapi.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openapi.backend.entity.RateLimitConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface RateLimitConfigMapper extends BaseMapper<RateLimitConfig> {

    @Delete("DELETE FROM rate_limit_config WHERE interface_id = #{interfaceId}")
    int physicalDeleteByInterface(@Param("interfaceId") Long interfaceId);
}
