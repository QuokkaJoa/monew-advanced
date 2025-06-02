package com.part2.monew.exception;

public class NoPermissionException extends RuntimeException {

  public NoPermissionException() {
    super("사용자를 수정할 권한이 없습니다.");
  }

}
