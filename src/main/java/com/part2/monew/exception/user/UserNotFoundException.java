package com.part2.monew.exception.user;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends UserException {

  public UserNotFoundException() {
      super("해당 사용자를 찾을 수 없습니다.");
  }

  @Override
  public String getErrorCode() {
    return "USER_NOT_FOUND";
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }
}
