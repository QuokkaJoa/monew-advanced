package com.part2.monew.dto.response;

import java.sql.Timestamp;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleViewResponseDto {
    
    private UUID id;
    private UUID viewedBy;
    private Timestamp createdAt;
    private UUID articleId;
    private String source;
    private String sourceUrl;
    private String articleTitle;
    private Timestamp articlePublishedDate;
    private String articleSummary;
    private Long articleCommentCount;
    private Long articleViewCount;
} 