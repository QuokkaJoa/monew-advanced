package com.part2.monew.controller;

import com.part2.monew.dto.request.NotificationCursorRequest;
import com.part2.monew.dto.response.CursorPageResponse;
import com.part2.monew.dto.response.NotificationResponse;
import com.part2.monew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PatchMapping("")
    public ResponseEntity<Void> updated_AllNotifications(@RequestHeader(value = "Monew-Request-User-ID", required = false) UUID userId){
            notificationService.updatedAll(userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{notificationId}")
    public ResponseEntity<Void> updated_Notifications(
            @RequestHeader(value = "Monew-Request-User-ID", required = false) UUID userId,
            @PathVariable UUID notificationId){
            notificationService.updated(notificationId, userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("")
    public ResponseEntity<CursorPageResponse<NotificationResponse>> info_Notification(
            @ModelAttribute NotificationCursorRequest request,
            @RequestHeader(value = "Monew-Request-User-ID", required = false) UUID userId
            ){

        CursorPageResponse<NotificationResponse> result = notificationService.getNoConfirmedNotifications(userId, request);
        return ResponseEntity.ok(result);
    }

}
