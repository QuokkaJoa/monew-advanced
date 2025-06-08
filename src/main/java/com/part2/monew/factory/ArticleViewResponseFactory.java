package com.part2.monew.factory;

import com.part2.monew.dto.response.ArticleViewResponseDto;
import com.part2.monew.entity.NewsArticle;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ArticleViewResponseFactory {
    
    public ArticleViewResponseDto createArticleViewResponse(NewsArticle article, UUID userId) {
        return ArticleViewResponseDto.builder()
            .id(UUID.randomUUID()) // 뷰 ID (새로 생성)
            .viewedBy(userId)
            .createdAt(new Timestamp(System.currentTimeMillis()))
            .articleId(article.getId())
            .source(article.getSourceIn())
            .sourceUrl(article.getSourceUrl())
            .articleTitle(article.getTitle())
            .articlePublishedDate(article.getPublishedDate())
            .articleSummary(article.getSummary())
            .articleCommentCount(article.getCommentCount())
            .articleViewCount(article.getViewCount())
            .build();
    }
} 