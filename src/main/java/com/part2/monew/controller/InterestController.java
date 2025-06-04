package com.part2.monew.controller;

import com.part2.monew.dto.request.InterestRegisterRequestDto;
import com.part2.monew.dto.request.InterestUpdateRequestDto;
import com.part2.monew.dto.response.InterestDto;
import com.part2.monew.service.InterestService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {

  private final InterestService interestService;

  @PostMapping
  public ResponseEntity<InterestDto> registerInterest(@Valid @RequestBody
      InterestRegisterRequestDto requestDto, @RequestHeader(value = "Monew-Request-User-ID",required = false)
      UUID requestUserId){
    InterestDto createdInterest = interestService.registerInterest(requestDto, requestUserId);

    return ResponseEntity.status(HttpStatus.CREATED).body(createdInterest);
  }

  @PatchMapping("/{interestId}")
  public ResponseEntity<InterestDto> updateInterestKeywords(@PathVariable UUID interestId, @Valid @RequestBody
      InterestUpdateRequestDto requestDto, @RequestHeader(value = "Monew-Request-User-Id", required = false) UUID requestUserId) {
    InterestDto updatedIntertest = interestService.updateInterestKeywords(interestId, requestDto,
        requestUserId);
    return ResponseEntity.ok(updatedIntertest);
  }
}
