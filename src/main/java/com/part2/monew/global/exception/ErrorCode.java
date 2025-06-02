package com.part2.monew.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  // Common Errors (Cxxx)
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드입니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
  INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "요청 값의 타입이 올바르지 않습니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다."),

  // Interest Errors (Ixxx)
  INTEREST_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "I001", "이미 존재하는 관심사 이름입니다."),
  SIMILAR_INTEREST_EXISTS(HttpStatus.CONFLICT, "I002", "매우 유사한 이름의 관심사가 이미 존재합니다. 다른 이름을 사용해주세요."),
  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "I003", "해당 관심사를 찾을 수 없습니다."),

  // Keyword Errors (Kxxx)
  KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "K001", "해당 키워드를 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

}
