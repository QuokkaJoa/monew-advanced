package com.part2.monew.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;
  private final String detailMessage;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.detailMessage = errorCode.getMessage();
  }

  public BusinessException(ErrorCode errorCode, String detailMessage) {
    super(detailMessage);
    this.errorCode = errorCode;
    this.detailMessage = detailMessage;
  }
}
