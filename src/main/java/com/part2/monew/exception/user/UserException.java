package com.part2.monew.exception.user;

import org.springframework.http.HttpStatus;

public abstract class UserException extends RuntimeException {

  public UserException(String message) {
    super(message);
  }

  public abstract String getErrorCode();
  public abstract HttpStatus getStatus();
}
