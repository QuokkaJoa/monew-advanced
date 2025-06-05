package com.part2.monew.controller;

import com.part2.monew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notificaitons")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PatchMapping("")
    public ResponseEntity<Void> updated_AllNotifications(@RequestHeader("MoNew-Request-User-ID") UUID userId){
            notificationService.updatedAll(userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{notificationId}")
    public ResponseEntity<Void> updated_Notifications(
            @RequestHeader("MoNew-Request-User-ID") UUID userId,
            @PathVariable UUID notificationId){
            notificationService.updated(notificationId, userId);

        return ResponseEntity.noContent().build();
    }

}
