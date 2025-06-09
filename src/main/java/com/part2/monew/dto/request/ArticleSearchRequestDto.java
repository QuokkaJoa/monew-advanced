package com.part2.monew.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSearchRequestDto {
    
    private String keyword;              // 검색어(제목, 요약)
    private String interestId;           // 관심사 ID (UUID를 String으로)
    private List<String> sourceIn;       // 출처(포함)
    private String publishDateFrom;      // 날짜 시작(범위)
    private String publishDateTo;        // 날짜 끝(범위)
    private String orderBy;              // 정렬 속성 이름 (publishDate, commentCount, viewCount)
    private String direction;            // 정렬 방향 (ASC, DESC)
    private String cursor;               // 커서 값
    private String after;                // 보조 커서(createdAt) 값
    private Integer limit;               // 커서 페이지 크기
    private String userId;               // 요청자 ID (헤더에서 받을 값)
} 