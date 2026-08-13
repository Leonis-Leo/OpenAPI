package com.openapi.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openapi.domain.entity.InterfaceSubscribe;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface InterfaceSubscribeMapper extends BaseMapper<InterfaceSubscribe> {

    /**
     * 物理删除（取消订阅后允许重新订阅，不受唯一键限制）。
     */
    @Delete("DELETE FROM interface_subscribe WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    /**
     * 数据面订阅校验：应用是否已通过某接口的订阅审批。
     */
    @Select("SELECT COUNT(*) FROM interface_subscribe WHERE app_id = #{appId} AND interface_id = #{interfaceId} AND status = 1 AND is_delete = 0")
    long countApproved(@Param("appId") Long appId, @Param("interfaceId") Long interfaceId);
}
