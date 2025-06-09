package com.part2.monew.global.exception.article;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class ArticleNotFoundException extends BusinessException {

  public ArticleNotFoundException() {
    super(ErrorCode.ARTICLE_NOT_FOUND);
  }
}
