package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openapi.domain.entity.App;
import com.openapi.domain.entity.InterfaceInfo;
import com.openapi.domain.entity.InterfaceSubscribe;
import com.openapi.domain.entity.User;
import com.openapi.domain.mapper.AppMapper;
import com.openapi.domain.mapper.InterfaceInfoMapper;
import com.openapi.domain.mapper.InterfaceSubscribeMapper;
import com.openapi.domain.mapper.UserMapper;
import com.openapi.backend.service.InterfaceSubscribeService;
import com.openapi.backend.service.NotificationService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterfaceSubscribeServiceImpl extends ServiceImpl<InterfaceSubscribeMapper, InterfaceSubscribe>
        implements InterfaceSubscribeService {

    private final AppMapper appMapper;
    private final InterfaceInfoMapper interfaceInfoMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        User user = userMapper.selectById(userId);
        String userAccount = user == null ? String.valueOf(userId) : user.getUserAccount();
        notificationService.notifyAdmins(
                "SUBSCRIBE_APPLY",
                "新的订阅申请",
                "开发者 " + userAccount + " 申请订阅接口「" + interfaceInfo.getName() + "」（应用：" + app.getAppName() + "）",
                subscribe.getId(),
                "/subscribes");
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
    public Page<Map<String, Object>> pageByUser(Long userId, long current, long size) {
        LambdaQueryWrapper<InterfaceSubscribe> query = new LambdaQueryWrapper<InterfaceSubscribe>()
                .eq(InterfaceSubscribe::getUserId, userId)
                .orderByDesc(InterfaceSubscribe::getId);
        Page<InterfaceSubscribe> source = page(new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 100)), query);
        return enrichPage(source);
    }

    @Override
    public Page<Map<String, Object>> pageByStatus(Integer status, long current, long size) {
        LambdaQueryWrapper<InterfaceSubscribe> query = new LambdaQueryWrapper<InterfaceSubscribe>()
                .orderByDesc(InterfaceSubscribe::getId);
        if (status != null) query.eq(InterfaceSubscribe::getStatus, status);
        Page<InterfaceSubscribe> source = page(new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 100)), query);
        return enrichPage(source);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long subscribeId, boolean approved) {
        InterfaceSubscribe subscribe = getById(subscribeId);
        if (subscribe == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订阅记录不存在");
        }
        subscribe.setStatus(approved ? 1 : 2);
        updateById(subscribe);
        InterfaceInfo interfaceInfo = interfaceInfoMapper.selectById(subscribe.getInterfaceId());
        String interfaceName = interfaceInfo == null ? "-" : interfaceInfo.getName();
        notificationService.notifyUser(
                subscribe.getUserId(),
                approved ? "SUBSCRIBE_APPROVED" : "SUBSCRIBE_REJECTED",
                approved ? "订阅申请已通过" : "订阅申请被拒绝",
                "您申请的接口「" + interfaceName + "」已" + (approved ? "通过审批，现在可以调用" : "被拒绝"),
                subscribe.getId(),
                "/interfaces");
    }

    @Override
    public void unsubscribe(Long userId, Long subscribeId) {
        InterfaceSubscribe subscribe = getById(subscribeId);
        if (subscribe == null || !subscribe.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订阅记录不存在或不属于当前用户");
        }
        baseMapper.physicalDeleteById(subscribeId);
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
            map.put("userId", item.getUserId());
            map.put("status", item.getStatus());
            map.put("createTime", item.getCreateTime());
            InterfaceInfo info = interfaceInfoMapper.selectById(item.getInterfaceId());
            map.put("interfaceName", info == null ? "-" : info.getName());
            map.put("interfaceUrl", info == null ? "-" : info.getUrl());
            App app = appMapper.selectById(item.getAppId());
            map.put("appName", app == null ? "-" : app.getAppName());
            User user = userMapper.selectById(item.getUserId());
            map.put("userAccount", user == null ? "-" : user.getUserAccount());
            return map;
        }).toList();
    }

    private Page<Map<String, Object>> enrichPage(Page<InterfaceSubscribe> source) {
        Page<Map<String, Object>> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(listWithNames(source.getRecords()));
        return result;
    }
}
