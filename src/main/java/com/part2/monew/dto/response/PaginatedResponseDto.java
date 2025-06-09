package com.part2.monew.dto.response;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponseDto<T> {
    private List<T> content;
    private String nextCursor;    // 다음 페이지 조회를 위한 주 커서 (예: ID)
    private Timestamp nextAfter;  // 다음 페이지 조회를 위한 보조 커서 (예: publishedDate)
    // 추가: viewCount 정렬 시 사용할 다음 보조 커서
    private Long nextCursorViewCount; 
    private int size;             // 현재 페이지의 아이템 수
    private long totalElements;   // 전체 아이템 수 (선택적, count 쿼리 필요할 수 있음)
    private boolean hasNext;      // 다음 페이지 존재 여부
} 