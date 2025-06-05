package com.part2.monew.global.exception.notification;

import com.part2.monew.global.exception.BusinessException;
import com.part2.monew.global.exception.ErrorCode;

public class NoParsingException extends BusinessException {
    public NoParsingException() {
        super(ErrorCode.NOTIFICATION_NO_PARSING, ErrorCode.NOTIFICATION_NO_PARSING.getMessage());
    }
}
