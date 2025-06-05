package com.part2.monew.global.exception.user;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class NoPermissionToDeleteException extends BusinessException {

  public NoPermissionToDeleteException() {
    super(ErrorCode.NO_PERMISSION_TO_DELETE, ErrorCode.NO_PERMISSION_TO_DELETE.getMessage());
  }
  public NoPermissionToDeleteException(String detailMessage) {
    super(ErrorCode.NO_PERMISSION_TO_DELETE, detailMessage);
  }

}
