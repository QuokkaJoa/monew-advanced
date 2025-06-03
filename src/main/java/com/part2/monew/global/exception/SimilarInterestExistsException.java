package com.part2.monew.global.exception;

public class SimilarInterestExistsException extends BusinessException{
  public SimilarInterestExistsException(String detailMessage) {
    super(ErrorCode.SIMILAR_INTEREST_EXISTS, detailMessage);
  }
}
