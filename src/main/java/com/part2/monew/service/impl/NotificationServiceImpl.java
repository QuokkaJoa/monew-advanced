package com.part2.monew.service.impl;

import com.part2.monew.entity.Notification;
import com.part2.monew.entity.User;
import com.part2.monew.repository.NotificationRepository;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;


    @Override
    public Notification createNotification(User user, String content, String resourceType, UUID resourceId) {
        Notification notification = new Notification(user, content, resourceType, resourceId);

        return notificationRepository.save(notification);
    }
}
