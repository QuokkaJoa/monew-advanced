package com.part2.monew.controller;

import com.part2.monew.dto.response.UserActivityResponse;
import com.part2.monew.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-activities")
public class UserActivityController {

  private final UserActivityService userActivityService;

  @GetMapping("/{userId}")
  public ResponseEntity<UserActivityResponse> getUserActivity(@PathVariable UUID userId) {
    return ResponseEntity.ok(userActivityService.getUserActivity(userId));
  }
}
