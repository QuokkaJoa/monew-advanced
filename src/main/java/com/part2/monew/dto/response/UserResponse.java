package com.part2.monew.dto.response;

import java.sql.Timestamp;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String nickname,
        Timestamp createdAt
) {
}
