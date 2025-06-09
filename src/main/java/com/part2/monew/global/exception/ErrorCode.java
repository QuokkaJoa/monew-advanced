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

  // User Errors (Uxxx)
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "해당 사용자를 찾을 수 없습니다."),
  EMAIL_DUPLICATED(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),
  NO_PERMISSION_TO_UPDATE(HttpStatus.FORBIDDEN, "U003", "사용자를 수정할 권한이 없습니다."),
  NO_PERMISSION_TO_DELETE(HttpStatus.FORBIDDEN, "U004", "사용자를 삭제할 권한이 없습니다."),

  // Interest Errors (Ixxx)
  INTEREST_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "I001", "이미 존재하는 관심사 이름입니다."),
  SIMILAR_INTEREST_EXISTS(HttpStatus.CONFLICT, "I002", "매우 유사한 이름의 관심사가 이미 존재합니다. 다른 이름을 사용해주세요."),
  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "I003", "해당 관심사를 찾을 수 없습니다."),
  ALREADY_SUBSCRIBED_INTEREST(HttpStatus.CONFLICT, "I004", "이미 구독한 관심사입니다."),

  // Keyword Errors (Kxxx)
  KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "K001", "해당 키워드를 찾을 수 없습니다."),

  // Article Errors(Axxx)
  ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "해당 기사를 찾을 수 없습니다."),
  ARTICLE_SOURCE_INVALID(HttpStatus.BAD_REQUEST, "A002", "유효하지 않은 뉴스 소스입니다."),
  ARTICLE_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A003", "기사 검색 중 오류가 발생했습니다."),
  INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "A004", "날짜 범위가 올바르지 않습니다."),
  INVALID_PAGINATION_PARAMETER(HttpStatus.BAD_REQUEST, "A005", "페이지네이션 파라미터가 올바르지 않습니다."),
  ARTICLE_BACKUP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A006", "기사 백업 중 오류가 발생했습니다."),
  ARTICLE_RESTORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A007", "기사 복구 중 오류가 발생했습니다."),
  ARTICLE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A008", "기사 삭제 중 오류가 발생했습니다."),
  ARTICLE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "A009", "이미 삭제된 기사입니다."),

  // Comment Errors (Cxxx)
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "해당 댓글을 찾을 수 없습니다."),
  COMMENT_LIKE_DUPLICATION(HttpStatus.BAD_REQUEST, "C002", "이미 좋아요가 되어있습니다."),
  COMMENT_UNLIKE_DUPLICATION(HttpStatus.BAD_REQUEST, "C003", "이미 좋아요 취소가 되어있습니다."),
  COMMENT_IS_ACTIVE(HttpStatus.BAD_REQUEST, "C004", "댓글 삭제 실패하였습니다."),

  // Notification Errors (Nxxx)
  NOTIFICATION_NO_PARSING(HttpStatus.NOT_FOUND, "N001", "Cursor 값이 잘못된 형식입니다.");


  private final HttpStatus status;
  private final String code;
  private final String message;

}
