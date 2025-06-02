package com.part2.monew.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ValidationExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorListResponse> handleValidationError(MethodArgumentNotValidException ex) {
    List<String> messages = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": "+ error.getDefaultMessage())
        .toList();

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorListResponse("VALIDATION_FAILED", messages));
  }

  public record ErrorListResponse(String code, List<String> messages) {}

}
