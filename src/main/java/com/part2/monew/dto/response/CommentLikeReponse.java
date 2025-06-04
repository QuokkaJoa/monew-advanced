package com.part2.monew.dto.response;

import com.part2.monew.entity.CommentLike;
import com.part2.monew.entity.CommentsManagement;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
public class CommentLikeReponse {

    private UUID id;

    private UUID likeBy;

    private Timestamp createdAt;

    private UUID commentId;

    private UUID articleId;

    private UUID commentUserId;

    private String commentUserNickname;

    private String content;

    private int LikeCount;

    private Timestamp commentCreatedAt;

    @Builder
    private CommentLikeReponse(UUID id, UUID likeBy, Timestamp createdAt, UUID commentId, UUID articleId, UUID commentUserId, String commentUserNickname, String content, int likeCount, Timestamp commentCreatedAt) {
        this.id = id;
        this.likeBy = likeBy;
        this.createdAt = createdAt;
        this.commentId = commentId;
        this.articleId = articleId;
        this.commentUserId = commentUserId;
        this.commentUserNickname = commentUserNickname;
        this.content = content;
        LikeCount = likeCount;
        this.commentCreatedAt = commentCreatedAt;
    }

    public static CommentLikeReponse of(CommentsManagement commentsManagement, CommentLike commentLike) {
        return CommentLikeReponse.builder()
                .id(commentLike.getId())
                .likeBy(commentLike.getId())
                .createdAt(commentLike.getCreatedAt())
                .commentId(commentLike.getCommentsManagement().getId())
                .articleId(commentsManagement.getNewsArticle().getId())
                .commentUserId(commentsManagement.getUser().getId())
                .commentUserNickname(commentsManagement.getUser().getUsername())
                .content(commentsManagement.getContent())
                .likeCount(commentsManagement.getLikeCount())
                .commentCreatedAt(commentsManagement.getCreatedAt())
                .build();
    }
}
