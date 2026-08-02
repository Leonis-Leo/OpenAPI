package com.openapi.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openapi.backend.entity.InterfaceSubscribe;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface InterfaceSubscribeMapper extends BaseMapper<InterfaceSubscribe> {

    /**
     * 物理删除（取消订阅后允许重新订阅，不受唯一键限制）。
     */
    @Delete("DELETE FROM interface_subscribe WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
