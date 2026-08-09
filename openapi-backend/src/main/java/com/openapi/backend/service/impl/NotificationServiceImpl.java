package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.backend.entity.Notification;
import com.openapi.backend.entity.User;
import com.openapi.backend.mapper.NotificationMapper;
import com.openapi.backend.mapper.UserMapper;
import com.openapi.backend.service.NotificationService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    private final UserMapper userMapper;

    @Override
    public Page<Notification> pageByUser(Long userId, Integer read, long current, long size) {
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        if (read != null) {
            query.eq(Notification::getIsRead, read);
        }
        query.orderByDesc(Notification::getIsRead).orderByDesc(Notification::getId);
        return page(new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 100)), query);
    }

    @Override
    public long unreadCount(Long userId) {
        return lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .count();
    }

    @Override
    public void markRead(Long userId, Long id) {
        Notification notification = getById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "通知不存在");
        }
        if (notification.getIsRead() == null || notification.getIsRead() == 0) {
            notification.setIsRead(1);
            updateById(notification);
        }
    }

    @Override
    public void markAllRead(Long userId) {
        lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1)
                .update();
    }

    @Override
    public void delete(Long userId, Long id) {
        Notification notification = getById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "通知不存在");
        }
        removeById(id);
    }

    @Override
    public void clearAll(Long userId) {
        lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .remove();
    }

    @Override
    public void notifyAdmins(String type, String title, String content, Long bizId, String link) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getUserRole, "admin")
                .eq(User::getStatus, 1));
        admins.forEach(admin -> notifyUser(admin.getId(), type, title, content, bizId, link));
    }

    @Override
    public void notifyUser(Long userId, String type, String title, String content, Long bizId, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizId(bizId);
        notification.setLink(link);
        notification.setIsRead(0);
        notification.setIsDelete(0);
        save(notification);
    }
}
