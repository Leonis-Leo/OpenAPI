package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.backend.entity.App;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.entity.InterfaceSubscribe;
import com.openapi.backend.mapper.AppMapper;
import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.mapper.InterfaceSubscribeMapper;
import com.openapi.backend.service.InterfaceSubscribeService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterfaceSubscribeServiceImpl extends ServiceImpl<InterfaceSubscribeMapper, InterfaceSubscribe>
        implements InterfaceSubscribeService {

    private final AppMapper appMapper;
    private final InterfaceInfoMapper interfaceInfoMapper;

    @Override
    public InterfaceSubscribe subscribe(Long userId, Long interfaceId, Long appId) {
        InterfaceInfo interfaceInfo = interfaceInfoMapper.selectById(interfaceId);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        App app = appMapper.selectById(appId);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用不存在或不属于当前用户");
        }
        InterfaceSubscribe existing = lambdaQuery()
                .eq(InterfaceSubscribe::getInterfaceId, interfaceId)
                .eq(InterfaceSubscribe::getAppId, appId)
                .one();
        if (existing != null && existing.getStatus() == 2) {
            // 被拒绝后允许重新申请
            existing.setStatus(0);
            updateById(existing);
            return existing;
        }
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已提交过该接口的订阅申请");
        }
        InterfaceSubscribe subscribe = new InterfaceSubscribe();
        subscribe.setInterfaceId(interfaceId);
        subscribe.setAppId(appId);
        subscribe.setUserId(userId);
        subscribe.setStatus(0);
        subscribe.setIsDelete(0);
        save(subscribe);
        return subscribe;
    }

    @Override
    public List<Map<String, Object>> listByUser(Long userId) {
        return listWithNames(lambdaQuery().eq(InterfaceSubscribe::getUserId, userId).list());
    }

    @Override
    public List<Map<String, Object>> listByStatus(Integer status) {
        if (status == null) {
            return listWithNames(list());
        }
        return listWithNames(lambdaQuery().eq(InterfaceSubscribe::getStatus, status).list());
    }

    @Override
    public void approve(Long subscribeId, boolean approved) {
        InterfaceSubscribe subscribe = getById(subscribeId);
        if (subscribe == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订阅记录不存在");
        }
        subscribe.setStatus(approved ? 1 : 2);
        updateById(subscribe);
    }

    @Override
    public boolean hasApprovedSubscription(Long appId, Long interfaceId) {
        long count = lambdaQuery()
                .eq(InterfaceSubscribe::getAppId, appId)
                .eq(InterfaceSubscribe::getInterfaceId, interfaceId)
                .eq(InterfaceSubscribe::getStatus, 1)
                .count();
        return count > 0;
    }

    private List<Map<String, Object>> listWithNames(List<InterfaceSubscribe> list) {
        return list.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("interfaceId", item.getInterfaceId());
            map.put("appId", item.getAppId());
            map.put("status", item.getStatus());
            map.put("createTime", item.getCreateTime());
            InterfaceInfo info = interfaceInfoMapper.selectById(item.getInterfaceId());
            map.put("interfaceName", info == null ? "-" : info.getName());
            map.put("interfaceUrl", info == null ? "-" : info.getUrl());
            App app = appMapper.selectById(item.getAppId());
            map.put("appName", app == null ? "-" : app.getAppName());
            return map;
        }).toList();
    }
}
