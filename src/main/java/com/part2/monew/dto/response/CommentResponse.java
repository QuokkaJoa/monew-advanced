package com.part2.monew.dto.response;


import com.part2.monew.entity.CommentsManagement;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
public class CommentResponse {
    private UUID id;
    private UUID articledId;
    private UUID userId;
    private String userNickname;
    private String content;
    private int likeCount;
    private Boolean likedByMe;
    private Timestamp createdAt;

    @Builder
    private CommentResponse(UUID id, UUID articledId, UUID userId, String userNickname, String content, int likeCount, boolean likedByMe, Timestamp createdAt) {
        this.id = id;
        this.articledId = articledId;
        this.userId = userId;
        this.userNickname = userNickname;
        this.content = content;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
        this.createdAt = createdAt;
    }

    public static CommentResponse of(CommentsManagement comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .articledId(comment.getNewsArticle().getId())
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getUsername())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .likedByMe( comment.getLikeCount() > 0)
                .createdAt(comment.getCreatedAt())
                .build();
    }

}
