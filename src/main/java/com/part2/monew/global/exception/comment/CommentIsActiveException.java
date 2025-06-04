package com.part2.monew.global.exception.comment;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class CommentIsActiveException extends BusinessException {

  public CommentIsActiveException() {
    super(ErrorCode.COMMENT_IS_ACTIVE, ErrorCode.COMMENT_IS_ACTIVE.getMessage());
  }

  public CommentIsActiveException(String detailMessage) {
    super(ErrorCode.COMMENT_IS_ACTIVE, detailMessage);
  }
}
