package com.financialcrm.service;

import com.financialcrm.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    Notification createNotification(Long userId, String title, String message, Notification.NotificationType type);
    List<Notification> getUnreadNotifications(Long userId);
    long countUnread(Long userId);
    void markAllAsRead(Long userId);
    void markAsRead(Long notificationId);
    Page<Notification> findAll(Long userId, Pageable pageable);
}
