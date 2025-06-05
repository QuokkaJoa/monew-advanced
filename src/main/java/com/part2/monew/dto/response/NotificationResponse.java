package com.part2.monew.dto.response;

import java.sql.Timestamp;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        Timestamp createdAt,
        Timestamp updatedAt,
        boolean confirmed,
        UUID userId,
        String content,
        String resourceType,
        UUID resourceId
) {}
