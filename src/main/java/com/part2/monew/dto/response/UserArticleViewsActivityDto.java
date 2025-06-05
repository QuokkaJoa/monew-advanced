package com.part2.monew.dto.response;

import com.part2.monew.entity.ActivityDetail;
import com.part2.monew.entity.NewsArticle;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Builder
public class UserArticleViewsActivityDto {
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

  public static UserArticleViewsActivityDto of(ActivityDetail activity) {
    NewsArticle article = activity.getNewsArticle();
    return UserArticleViewsActivityDto.builder()
        .id(activity.getId())
        .viewedBy(activity.getUser().getId())
        .createdAt(activity.getViewedAt())
        .articleId(article.getId())
        .source("NAVER")
        .sourceUrl(article.getSourceUrl())
        .articleTitle(article.getTitle())
        .articlePublishedDate(article.getPublishedDate())
        .articleSummary(article.getSummary())
        .articleCommentCount((long) article.getComments().size())
        .articleViewCount(article.getViewCount())
        .build();
  }
}