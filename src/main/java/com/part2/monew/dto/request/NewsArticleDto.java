package com.part2.monew.dto.request;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleDto {
    private String providerName;      // 출처 제공자 이름 (예: "Naver News API", "조선일보 RSS")
    private String title;             // 기사 제목
    private String originalLink;      // 원본 기사 URL
    private Timestamp publishedDate; // 발행 일시
    private String summaryOrContent;  // 요약
    private String guid;              // RSS의 경우 전역 고유 식별자
    private String thumbnailUrl;
} 