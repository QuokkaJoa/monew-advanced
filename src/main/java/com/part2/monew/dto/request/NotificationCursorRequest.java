package com.part2.monew.dto.request;

import java.util.UUID;

public record NotificationCursorRequest(
        String cursor,
        String after,
        Integer limit
) {}
