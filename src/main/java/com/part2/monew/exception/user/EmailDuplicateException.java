package com.part2.monew.exception.user;

public class EmailDuplicateException extends RuntimeException {

  public EmailDuplicateException() {
    super("이미 사용 중인 이메일입니다.");
  }

}
