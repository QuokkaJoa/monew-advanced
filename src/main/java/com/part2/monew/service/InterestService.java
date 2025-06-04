package com.part2.monew.service;

import com.part2.monew.dto.request.InterestRegisterRequestDto;
import com.part2.monew.dto.request.InterestUpdateRequestDto;
import com.part2.monew.dto.response.InterestDto;
import java.util.UUID;

public interface InterestService {

  InterestDto registerInterest(InterestRegisterRequestDto requestDto, UUID requestUserId);

  InterestDto updateInterestKeywords(UUID interestId, InterestUpdateRequestDto requestDto, UUID requestId);

}
