package com.part2.monew.global.exception.interest;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class SimilarInterestExistsException extends BusinessException {
  public SimilarInterestExistsException(String detailMessage) {
    super(ErrorCode.SIMILAR_INTEREST_EXISTS, detailMessage);
  }
}
