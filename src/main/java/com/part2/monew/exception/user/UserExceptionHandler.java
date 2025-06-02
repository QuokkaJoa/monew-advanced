package com.part2.monew.exception.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

  @ExceptionHandler(EmailDuplicateException.class)
  public ResponseEntity<ErrorResponse> handleEmailDuplicate(EmailDuplicateException ex) {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("EMAIL_DUPLICATED", ex.getMessage()));
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(NoPermissionException.class)
  public ResponseEntity<ErrorResponse> handleNoPermission(NoPermissionException ex) {
    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("NO_PERMISSION", ex.getMessage()));
  }

  public record ErrorResponse(String code, String message) {}
}
