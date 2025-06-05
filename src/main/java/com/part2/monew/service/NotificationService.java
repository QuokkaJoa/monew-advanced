package com.part2.monew.service;

import com.part2.monew.entity.Notification;
import com.part2.monew.entity.QNotification;
import com.part2.monew.entity.User;

import java.util.UUID;

public interface NotificationService {
    Notification createNotification(User user, String content, String resourceType, UUID resourceId);
    void updatedAll(UUID notificationId);
    void updated(UUID notificationId, UUID userId);

}
