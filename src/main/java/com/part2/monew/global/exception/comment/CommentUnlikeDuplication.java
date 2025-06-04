package com.part2.monew.global.exception.comment;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class CommentUnlikeDuplication extends BusinessException {

  public CommentUnlikeDuplication() {
    super(ErrorCode.COMMENT_UNLIKE_DUPLICATION, ErrorCode.COMMENT_UNLIKE_DUPLICATION.getMessage());
  }

  public CommentUnlikeDuplication(String detailMessage) {
    super(ErrorCode.COMMENT_UNLIKE_DUPLICATION, detailMessage);
  }
}
