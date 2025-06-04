package com.part2.monew.global.exception.comment;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class CommentNotFoundException extends BusinessException {

  public CommentNotFoundException() {
    super(ErrorCode.COMMENT_NOT_FOUND, ErrorCode.COMMENT_NOT_FOUND.getMessage());
  }

  public CommentNotFoundException(String detailMessage) {
    super(ErrorCode.COMMENT_NOT_FOUND, detailMessage);
  }
}
