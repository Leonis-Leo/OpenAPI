package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.InterfaceSubscribe;

import java.util.List;
import java.util.Map;

public interface InterfaceSubscribeService extends IService<InterfaceSubscribe> {

    InterfaceSubscribe subscribe(Long userId, Long interfaceId, Long appId);

    List<Map<String, Object>> listByUser(Long userId);

    List<Map<String, Object>> listByStatus(Integer status);

    void approve(Long subscribeId, boolean approved);

    boolean hasApprovedSubscription(Long appId, Long interfaceId);
}
