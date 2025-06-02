package com.part2.monew.exception.user;

import org.springframework.http.HttpStatus;

public class NoPermissionToDeleteException extends UserException {

  public NoPermissionToDeleteException() {
    super("사용자를 삭제할 권한이 없습니다.");
  }

  @Override
  public String getErrorCode() {
    return "NO_PERMISSION_TO_DELETE";
  }

  @Override
  public HttpStatus getStatus(){
    return HttpStatus.FORBIDDEN;
  }
}
