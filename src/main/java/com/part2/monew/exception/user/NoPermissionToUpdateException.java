package com.part2.monew.exception.user;

import org.springframework.http.HttpStatus;

public class NoPermissionToUpdateException extends UserException {

  public NoPermissionToUpdateException() {
    super("사용자를 수정할 권한이 없습니다.");
  }

  @Override
  public String getErrorCode() {
    return "NO_PERMISSION_TO_UPDATE";
  }

  @Override
  public HttpStatus getStatus(){
    return HttpStatus.FORBIDDEN;
  }
}
