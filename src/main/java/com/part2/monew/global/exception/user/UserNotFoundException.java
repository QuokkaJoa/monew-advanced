package com.part2.monew.global.exception.user;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {

  public UserNotFoundException(String detailMessage) {
    super(ErrorCode.USER_NOT_FOUND, detailMessage);
  }

  public UserNotFoundException() {
    super(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage());
  }
}
