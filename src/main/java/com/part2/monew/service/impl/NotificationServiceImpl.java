package com.part2.monew.service.impl;

import com.part2.monew.entity.Notification;
import com.part2.monew.entity.User;
import com.part2.monew.global.exception.user.NoPermissionToUpdateException;
import com.part2.monew.repository.NotificationRepository;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public Notification createNotification(User user, String content, String resourceType, UUID resourceId) {
        Notification notification = new Notification(user, content, resourceType, resourceId);

        return notificationRepository.save(notification);
    }

    @Transactional
    @Override
    public void updated(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);

        if(!Objects.requireNonNull(notification).getUser().getId().equals(userId)){
            throw new NoPermissionToUpdateException("알림 확인 권한이 없습니다.");
        }
        notification.setConfirmed(true);

    }

    @Transactional
    @Override
    public void updatedAll(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndConfirmedFalse(userId);
        for (Notification n : notifications) {
            n.setConfirmed(true);
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void deleteOldConfirmedNotifications() {
        Timestamp oneWeekAgo = Timestamp.valueOf(LocalDateTime.now().minusWeeks(1));
        notificationRepository.deleteConfirmedNotificationsBefore(oneWeekAgo);
    }

}
