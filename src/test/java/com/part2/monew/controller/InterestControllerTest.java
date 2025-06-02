package com.part2.monew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part2.monew.dto.request.InterestRegisterRequestDto;
import com.part2.monew.dto.response.InterestDto;
import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;
import com.part2.monew.global.exception.SimilarInterestExistsException;
import com.part2.monew.service.InterestService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterestController.class)
class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private InterestService interestService;

  private final String BASE_URL = "/api/interests";

  @Test
  @DisplayName("관심사 등록 성공")
  void registerInterest_success() throws Exception {
    UUID requestUserId = UUID.randomUUID();
    InterestRegisterRequestDto requestDto = new InterestRegisterRequestDto("스포츠", Arrays.asList("야구", "축구"));
    InterestDto mockResponseDto = new InterestDto(UUID.randomUUID(), "스포츠", Arrays.asList("야구", "축구"), 0L, false);

    given(interestService.registerInterest(any(InterestRegisterRequestDto.class), eq(requestUserId)))
        .willReturn(mockResponseDto);

    ResultActions resultActions = mockMvc.perform(post(BASE_URL)
            .header("Monew-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
        .andDo(print());

    resultActions
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(mockResponseDto.id().toString()))
        .andExpect(jsonPath("$.name").value(mockResponseDto.name()))
        .andExpect(jsonPath("$.keywords[0]").value(mockResponseDto.keywords().get(0)))
        .andExpect(jsonPath("$.subscriberCount").value(mockResponseDto.subscriberCount()))
        .andExpect(jsonPath("$.subscribedByMe").value(mockResponseDto.subscribedByMe()));

    verify(interestService).registerInterest(any(InterestRegisterRequestDto.class), eq(requestUserId));
  }

  @Test
  @DisplayName("관심사 등록 실패 - 요청 DTO 유효성 검사 (이름 누락)")
  void registerInterest_fail_validation_nameBlank() throws Exception {
    UUID requestUserId = UUID.randomUUID();
    InterestRegisterRequestDto requestDtoWithBlankName = new InterestRegisterRequestDto("", Arrays.asList("야구", "축구"));

    ResultActions resultActions = mockMvc.perform(post(BASE_URL)
            .header("Monew-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDtoWithBlankName)))
        .andDo(print());

    resultActions
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
        .andExpect(jsonPath("$.fieldErrors").isArray())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.fieldErrors[0].reason").value("관심사 이름은 필수입니다."));
  }

  @Test
  @DisplayName("관심사 등록 실패 - 요청 DTO 유효성 검사 (키워드 누락)")
  void registerInterest_fail_validation_keywordsEmpty() throws Exception {
    UUID requestUserId = UUID.randomUUID();
    InterestRegisterRequestDto requestDtoWithEmptyKeywords = new InterestRegisterRequestDto("게임", List.of());

    ResultActions resultActions = mockMvc.perform(post(BASE_URL)
            .header("Monew-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDtoWithEmptyKeywords)))
        .andDo(print());

    resultActions
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
        .andExpect(jsonPath("$.fieldErrors").isArray())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("keywords"))
        .andExpect(jsonPath("$.fieldErrors[0].reason").value("키워드는 최소 1개 이상 등록해야 합니다."));
  }

  @Test
  @DisplayName("관심사 등록 실패 - 이미 존재하는 이름 (정확히 일치)")
  void registerInterest_fail_exactNameExists() throws Exception {
    UUID requestUserId = UUID.randomUUID();
    InterestRegisterRequestDto requestDto = new InterestRegisterRequestDto("스포츠", Arrays.asList("야구", "축구"));
    String errorMessage = String.format("이미 존재하는 관심사 이름입니다: %s", requestDto.name());

    given(interestService.registerInterest(any(InterestRegisterRequestDto.class), eq(requestUserId)))
        .willThrow(new BusinessException(ErrorCode.INTEREST_NAME_ALREADY_EXISTS, errorMessage));

    ResultActions resultActions = mockMvc.perform(post(BASE_URL)
            .header("Monew-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
        .andDo(print());

    resultActions
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.INTEREST_NAME_ALREADY_EXISTS.getCode()))
        .andExpect(jsonPath("$.message").value(errorMessage))
        .andExpect(jsonPath("$.status").value(ErrorCode.INTEREST_NAME_ALREADY_EXISTS.getStatus().value()));
  }

  @Test
  @DisplayName("관심사 등록 실패 - 유사한 이름 존재")
  void registerInterest_fail_similarNameExists() throws Exception {
    UUID requestUserId = UUID.randomUUID();
    InterestRegisterRequestDto requestDto = new InterestRegisterRequestDto("스포오츠", Arrays.asList("야구", "축구")); // 약간 다른 이름
    String similarExistingName = "스포츠";
    double similarity = 85.00;
    String errorMessage = String.format("유사한 이름의 관심사 '%s'가(이) 이미 존재합니다 (유사도: %.2f%%). 다른 이름을 사용해주세요.", similarExistingName, similarity);

    given(interestService.registerInterest(any(InterestRegisterRequestDto.class), eq(requestUserId)))
        .willThrow(new SimilarInterestExistsException(errorMessage));

    ResultActions resultActions = mockMvc.perform(post(BASE_URL)
            .header("Monew-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
        .andDo(print());

    resultActions
        .andExpect(status().isConflict()) // ErrorCode.SIMILAR_INTEREST_EXISTS의 status (409 Conflict)
        .andExpect(jsonPath("$.code").value(ErrorCode.SIMILAR_INTEREST_EXISTS.getCode()))
        .andExpect(jsonPath("$.message").value(errorMessage))
        .andExpect(jsonPath("$.status").value(ErrorCode.SIMILAR_INTEREST_EXISTS.getStatus().value()));
  }
}
