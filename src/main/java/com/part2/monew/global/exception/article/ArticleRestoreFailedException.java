package com.part2.monew.global.exception.article;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class ArticleRestoreFailedException extends BusinessException {

    public ArticleRestoreFailedException() {
        super(ErrorCode.ARTICLE_RESTORE_FAILED);
    }
} 