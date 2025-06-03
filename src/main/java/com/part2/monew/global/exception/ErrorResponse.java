package com.part2.monew.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 응답에서 제외
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    List<CustomFieldError> fieldErrors
) {

  public static ErrorResponse of(ErrorCode errorCode, String path) {
    return new ErrorResponse(LocalDateTime.now(), errorCode.getStatus().value(), errorCode.getStatus().getReasonPhrase(),
        errorCode.getCode(), errorCode.getMessage(), path, null);
  }

  public static ErrorResponse of(ErrorCode errorCode, String path, String customMessage) {
    return new ErrorResponse(LocalDateTime.now(), errorCode.getStatus().value(), errorCode.getStatus().getReasonPhrase(),
        errorCode.getCode(), customMessage != null ? customMessage : errorCode.getMessage(), path, null);
  }

  public static ErrorResponse of(ErrorCode errorCode, String path, BindingResult bindingResult) {
    return new ErrorResponse(LocalDateTime.now(), errorCode.getStatus().value(), errorCode.getStatus().getReasonPhrase(),
        errorCode.getCode(), errorCode.getMessage(), path, CustomFieldError.from(bindingResult));
  }

  public record CustomFieldError(
      String field,
      String value,
      String reason
  ) {
    public static List<CustomFieldError> from(BindingResult bindingResult) {
      final List<FieldError> errors = bindingResult.getFieldErrors();
      return errors.stream()
          .map(error -> new CustomFieldError(
              error.getField(),
              error.getRejectedValue() != null ? error.getRejectedValue().toString() : "",
              error.getDefaultMessage()))
          .collect(Collectors.toList());
    }
  }
}
