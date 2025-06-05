package com.part2.monew.service;

import com.part2.monew.dto.response.UserActivityResponse;
import java.util.UUID;

public interface UserActivityService {
    UserActivityResponse getUserActivity(UUID userId);

}
