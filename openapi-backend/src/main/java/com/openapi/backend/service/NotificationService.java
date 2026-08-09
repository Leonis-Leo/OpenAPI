package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.Notification;

public interface NotificationService extends IService<Notification> {

    Page<Notification> pageByUser(Long userId, Integer read, long current, long size);

    long unreadCount(Long userId);

    void markRead(Long userId, Long id);

    void markAllRead(Long userId);

    void delete(Long userId, Long id);

    void clearAll(Long userId);

    /** 给所有启用的管理员发送通知 */
    void notifyAdmins(String type, String title, String content, Long bizId, String link);

    /** 给指定用户发送通知 */
    void notifyUser(Long userId, String type, String title, String content, Long bizId, String link);
}
