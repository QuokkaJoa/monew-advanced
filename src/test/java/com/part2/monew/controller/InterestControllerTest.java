package com.part2.monew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.part2.monew.dto.request.InterestRegisterRequestDto;
import com.part2.monew.dto.request.InterestUpdateRequestDto;
import com.part2.monew.dto.response.InterestDto;
import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;
import com.part2.monew.global.exception.ErrorResponse;
import com.part2.monew.global.exception.GlobalExceptionHandler;
import com.part2.monew.global.exception.SimilarInterestExistsException;
import com.part2.monew.service.InterestService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterestController.class)
@Import(GlobalExceptionHandler.class)
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
  @DisplayName("관심사 등록 실패 - 요청 DTO 유효성 검사 (키워드 목록 비어있음)")
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
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));

    String responseBody = resultActions.andReturn().getResponse().getContentAsString();
    ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

    assertThat(errorResponse.status()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getStatus().value());
    assertThat(errorResponse.message()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    assertThat(errorResponse.path()).isEqualTo(BASE_URL);

    assertThat(errorResponse.fieldErrors())
        .isNotEmpty()
        .anySatisfy(fieldError -> {
          assertThat(fieldError.field()).isEqualTo("keywords");
          assertThat(fieldError.reason()).isEqualTo("키워드는 최소 1개 이상 등록해야 합니다.");
        });

    assertThat(errorResponse.fieldErrors())
        .anySatisfy(fieldError -> {
          assertThat(fieldError.field()).isEqualTo("keywords");
          assertThat(fieldError.reason()).isEqualTo("키워드는 1개 이상 10개 이하로 등록할 수 있습니다.");
        });

    assertThat(errorResponse.fieldErrors()).hasSize(2);
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
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.SIMILAR_INTEREST_EXISTS.getCode()))
        .andExpect(jsonPath("$.message").value(errorMessage))
        .andExpect(jsonPath("$.status").value(ErrorCode.SIMILAR_INTEREST_EXISTS.getStatus().value()));
  }

  @Test
  @DisplayName("관심사 키워드 수정 성공")
  void updateInterestKeywords_success() throws Exception {

    UUID interestId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();
    InterestUpdateRequestDto requestDto = new InterestUpdateRequestDto(Arrays.asList("농구", "배구"));

    InterestDto mockUpdatedInterestDto = new InterestDto(
        interestId,
        "스포츠",
        Arrays.asList("농구", "배구"),
        5L,
        true
    );

    given(interestService.updateInterestKeywords(eq(interestId), any(InterestUpdateRequestDto.class), eq(requestUserId)))
        .willReturn(mockUpdatedInterestDto);

    ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{interestId}", interestId)
            .header("Monew-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
        .andDo(print());

    resultActions
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(interestId.toString()))
        .andExpect(jsonPath("$.name").value(mockUpdatedInterestDto.name()))
        .andExpect(jsonPath("$.keywords[0]").value("농구"))
        .andExpect(jsonPath("$.keywords[1]").value("배구"))
        .andExpect(jsonPath("$.subscriberCount").value(mockUpdatedInterestDto.subscriberCount()))
        .andExpect(jsonPath("$.subscribedByMe").value(mockUpdatedInterestDto.subscribedByMe()));

    verify(interestService).updateInterestKeywords(eq(interestId), any(InterestUpdateRequestDto.class), eq(requestUserId));
  }

  @Test
  @DisplayName("관심사 키워드 수정 실패 - 요청 DTO 유효성 검사 (키워드 목록 비어있음)")
  void updateInterestKeywords_fail_validation_keywordsEmpty() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID(); // 테스트에서 사용되므로 선언 및 초기화
    InterestUpdateRequestDto requestDtoWithEmptyKeywords = new InterestUpdateRequestDto(Collections.emptyList()); // 빈 키워드 목록

    // when
    ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{interestId}", interestId)
            .header("Monew-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDtoWithEmptyKeywords)))
        .andDo(print());

    resultActions
        .andExpect(status().isBadRequest()) // HTTP 400 상태 코드 확인
        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode())); // 우리가 정의한 에러 코드 확인

    // 응답 본문을 ErrorResponse 객체로 변환하여 상세 검증
    String responseBody = resultActions.andReturn().getResponse().getContentAsString();
    ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

    assertThat(errorResponse.status()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getStatus().value());
    assertThat(errorResponse.message()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    assertThat(errorResponse.path()).isEqualTo(BASE_URL + "/" + interestId); // 요청 경로 확인

    // fieldErrors 리스트에 @NotEmpty 위반 메시지가 포함되어 있는지 확인
    assertThat(errorResponse.fieldErrors())
        .isNotEmpty() // 최소 하나 이상의 필드 에러가 있는지
        .anySatisfy(fieldError -> { // 리스트의 요소 중 하나라도 다음 조건을 만족하는지
          assertThat(fieldError.field()).isEqualTo("keywords");
          assertThat(fieldError.reason()).isEqualTo("키워드는 최소 1개 이상 등록해야 합니다."); // @NotEmpty 메시지
        });

    // fieldErrors 리스트에 @Size 위반 메시지가 포함되어 있는지 확인
    assertThat(errorResponse.fieldErrors())
        .anySatisfy(fieldError -> {
          assertThat(fieldError.field()).isEqualTo("keywords");
          assertThat(fieldError.reason()).isEqualTo("키워드는 1개이상 10개 이하로 등록 할 수 있습니다."); // @Size 메시지
        });

    // 추가적으로, fieldErrors 리스트의 크기가 2인지 확인할 수도 있습니다 (두 제약조건 모두 위반하므로)
    assertThat(errorResponse.fieldErrors()).hasSize(2);
  }

  @Test
  @DisplayName("관심사 키워드 수정 실패 - 존재하지 않는 관심사 ID")
  void updateInterestKeywords_fail_interestNotFound() throws Exception {
    UUID nonExistentInterestId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();
    InterestUpdateRequestDto requestDto = new InterestUpdateRequestDto(Arrays.asList("새키워드"));
    String errorMessage = String.format("수정할 관심사를 찾을 수 없습니다. ID: %s", nonExistentInterestId);

    given(interestService.updateInterestKeywords(eq(nonExistentInterestId), any(
        InterestUpdateRequestDto.class), eq(requestUserId)))
        .willThrow(new BusinessException(ErrorCode.INTEREST_NOT_FOUND, errorMessage));

    ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{interestId}", nonExistentInterestId)
            .header("Monew-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
        .andDo(print());

    resultActions
        .andExpect(status().isNotFound()) // ErrorCode.INTEREST_NOT_FOUND의 status (404 Not Found)
        .andExpect(jsonPath("$.code").value(ErrorCode.INTEREST_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.message").value(errorMessage))
        .andExpect(jsonPath("$.status").value(ErrorCode.INTEREST_NOT_FOUND.getStatus().value()));
  }
}
