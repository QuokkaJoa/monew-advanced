package com.part2.monew.service;

import com.part2.monew.entity.Notification;
import com.part2.monew.entity.User;

import java.util.UUID;

public interface NotificationService {
    Notification createNotification(User user, String content, String resourceType, UUID resourceId);

}
