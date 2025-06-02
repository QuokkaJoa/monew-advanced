package com.part2.monew.exception.user;

import org.springframework.http.HttpStatus;

public class EmailDuplicateException extends UserException {

  public EmailDuplicateException() {
    super("이미 사용 중인 이메일입니다.");
  }

  @Override
  public String getErrorCode() {
    return "EMAIL_DUPLICATED";
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }
}
