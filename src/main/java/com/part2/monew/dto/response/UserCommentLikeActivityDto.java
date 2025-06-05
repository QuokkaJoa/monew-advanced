package com.part2.monew.dto.response;

import com.part2.monew.entity.CommentLike;
import com.part2.monew.entity.CommentsManagement;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Builder
public class UserCommentLikeActivityDto {
  private UUID id;
  private Timestamp createdAt;
  private UUID commentId;
  private UUID articleId;
  private String articleTitle;
  private UUID commentUserId;
  private String commentUserNickname;
  private String commentContent;
  private int commentLikeCount;
  private Timestamp commentCreatedAt;

  public static UserCommentLikeActivityDto of(CommentLike commentLike) {
    CommentsManagement comment = commentLike.getCommentsManagement();

    return UserCommentLikeActivityDto.builder()
        .id(commentLike.getId())
        .createdAt(commentLike.getCreatedAt())
        .commentId(comment.getId())
        .articleId(comment.getNewsArticle().getId())
        .articleTitle(comment.getNewsArticle().getTitle())
        .commentUserId(comment.getUser().getId())
        .commentUserNickname(comment.getUser().getNickname())
        .commentContent(comment.getContent())
        .commentLikeCount(comment.getLikeCount())
        .commentCreatedAt(comment.getCreatedAt())
        .build();
  }
}