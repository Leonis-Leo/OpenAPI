package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openapi.domain.entity.InterfaceSubscribe;

import java.util.List;
import java.util.Map;

public interface InterfaceSubscribeService extends IService<InterfaceSubscribe> {

    InterfaceSubscribe subscribe(Long userId, Long interfaceId, Long appId);

    List<Map<String, Object>> listByUser(Long userId);

    List<Map<String, Object>> listByStatus(Integer status);

    Page<Map<String, Object>> pageByUser(Long userId, long current, long size);

    Page<Map<String, Object>> pageByStatus(Integer status, long current, long size);

    void approve(Long subscribeId, boolean approved);

    void unsubscribe(Long userId, Long subscribeId);

    boolean hasApprovedSubscription(Long appId, Long interfaceId);
}
