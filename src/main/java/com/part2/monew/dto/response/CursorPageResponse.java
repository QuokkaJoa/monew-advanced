package com.part2.monew.dto.response; // 또는 com.part2.monew.dto.common 등 공통 DTO 패키지

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 응답에서 제외 (선택 사항)
public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    String nextAfter,
    int size,
    Long totalElements,
    boolean hasNext
) {
  public static <T> CursorPageResponse<T> of(
      List<T> content, String nextCursor, String nextAfter,
      long totalElements, boolean hasNext) {
    return new CursorPageResponse<>(
        content,
        nextCursor,
        nextAfter,
        content != null ? content.size() : 0,
        totalElements,
        hasNext
    );
  }

  public static <T> CursorPageResponse<T> of(
      List<T> content, String nextCursor, String nextAfter, boolean hasNext) {
    return new CursorPageResponse<>(
        content,
        nextCursor,
        nextAfter,
        content != null ? content.size() : 0,
        null,
        hasNext
    );
  }
}
