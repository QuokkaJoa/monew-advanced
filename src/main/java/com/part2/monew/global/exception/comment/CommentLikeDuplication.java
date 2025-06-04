package com.part2.monew.global.exception.comment;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class CommentLikeDuplication extends BusinessException {

  public CommentLikeDuplication() {
    super(ErrorCode.COMMENT_LIKE_DUPLICATION, ErrorCode.COMMENT_LIKE_DUPLICATION.getMessage());
  }

  public CommentLikeDuplication(String detailMessage) {
    super(ErrorCode.COMMENT_LIKE_DUPLICATION, detailMessage);
  }
}
