package com.part2.monew.global.exception.article;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class ArticleBackupFailedException extends BusinessException {

    public ArticleBackupFailedException() {
        super(ErrorCode.ARTICLE_BACKUP_FAILED);
    }
} 