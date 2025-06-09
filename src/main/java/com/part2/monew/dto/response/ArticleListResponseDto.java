package com.part2.monew.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleListResponseDto<N> {
    
    private List<ArticleResponseDto> content;
    private String nextCursor;
    private LocalDateTime nextAfter;
    private Integer size;
    private Long totalElements;
    private Boolean hasNext;
} 