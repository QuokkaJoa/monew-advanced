package com.part2.monew.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record InterestSearchRequest(
    String keyword,
    @NotBlank(message = "정렬 기준(orderBy)은 필수입니다.")
    String orderBy,
    @NotBlank(message = "정렬 방향(direction)은 필수입니다.")
    String direction,
    String cursor,
    String after,
    @Min(value = 1, message = "페이지 크기(limit)는 1 이상이어야 합니다.")
    Integer limit
) {
  public InterestSearchRequest {
    if (limit == null) {
      limit = 50;
    }
  }
}
