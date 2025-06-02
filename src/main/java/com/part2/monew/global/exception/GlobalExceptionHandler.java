package com.part2.monew.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  protected ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException ex, HttpServletRequest request) {
    log.warn("handleBusinessException: {} (request path: {})", ex.getMessage(), request.getRequestURI(), ex);
    final ErrorCode errorCode = ex.getErrorCode();
    final ErrorResponse response = ErrorResponse.of(errorCode, request.getRequestURI(), ex.getDetailMessage());
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
    log.error("handleException: {} (request path: {})", ex.getMessage(), request.getRequestURI(), ex);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
    return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
  }
}
