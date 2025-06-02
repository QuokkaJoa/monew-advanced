package com.part2.monew.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@Getter
@JsonInclude(Include.NON_NULL)
public class ErrorResponse {
  private final LocalDateTime timestamp;
  private final int status;
  private final String error;
  private final String code;
  private final String message;
  private final String path;
  private List<CustomFieldError> fieldErrors;

  private ErrorResponse(ErrorCode errorCode, String path, String customMessage) {
    this.timestamp = LocalDateTime.now();
    this.status = errorCode.getStatus().value();
    this.error = errorCode.getStatus().getReasonPhrase();
    this.code = errorCode.getCode();
    this.message = customMessage != null ? customMessage : errorCode.getMessage();
    this.path = path;
  }

  private ErrorResponse(ErrorCode errorCode, String path, BindingResult bindingResult) {
    this.timestamp = LocalDateTime.now();
    this.status = errorCode.getStatus().value();
    this.error = errorCode.getStatus().getReasonPhrase();
    this.code = errorCode.getCode();
    this.message = errorCode.getMessage();
    this.path = path;
    this.fieldErrors = CustomFieldError.from(bindingResult);
  }

  public static ErrorResponse of(ErrorCode errorCode, String path) {
    return new ErrorResponse(errorCode, path, (String) null);
  }

  public static ErrorResponse of(ErrorCode errorCode, String path, String customMessage) {
    return new ErrorResponse(errorCode, path, customMessage);
  }

  public static ErrorResponse of(ErrorCode errorCode, String path, BindingResult bindingResult) {
    return new ErrorResponse(errorCode, path, bindingResult);
  }

  @Getter
  public static class CustomFieldError {
    private final String field;
    private final String value;
    private final String reason;

    private CustomFieldError(String field, String value, String reason) {
      this.field = field;
      this.value = value;
      this.reason = reason;
    }

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
