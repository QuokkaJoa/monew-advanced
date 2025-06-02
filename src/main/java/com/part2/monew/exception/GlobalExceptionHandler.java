package com.part2.monew.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EmailDuplicateException.class)
  public ResponseEntity<ErrorResponse> handleEmailDuplicate(EmailDuplicateException ex) {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("EMAIL_DUPLICATED", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorListResponse> handleValidationError(MethodArgumentNotValidException ex) {
    List<String> messages = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": "+ error.getDefaultMessage())
        .toList();

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorListResponse("VALIDATION_FAILED", messages));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
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
  public record ErrorListResponse(String code, List<String> messages) {}
}
