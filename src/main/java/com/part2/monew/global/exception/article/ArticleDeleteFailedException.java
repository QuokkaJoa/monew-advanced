package com.part2.monew.global.exception.article;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class ArticleDeleteFailedException extends BusinessException {

    public ArticleDeleteFailedException() {
        super(ErrorCode.ARTICLE_DELETE_FAILED);
    }
} 