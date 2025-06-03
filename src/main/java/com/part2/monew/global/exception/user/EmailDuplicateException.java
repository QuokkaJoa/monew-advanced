package com.part2.monew.global.exception.user;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class EmailDuplicateException extends BusinessException {

  public EmailDuplicateException(String detailMessage) {
    super(ErrorCode.EMAIL_DUPLICATED, detailMessage);
  }
}
