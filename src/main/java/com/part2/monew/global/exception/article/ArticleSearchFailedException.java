package com.part2.monew.global.exception.article;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class ArticleSearchFailedException extends BusinessException {

    public ArticleSearchFailedException() {
        super(ErrorCode.ARTICLE_SEARCH_FAILED);
    }
} 