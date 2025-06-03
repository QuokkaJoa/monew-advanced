package com.part2.monew.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CreateCommentRequest {

    @NotNull
    private UUID articleId;

    @NotNull
    private UUID userId;

    @NotNull
    private String content;

    @Builder
    private CreateCommentRequest(UUID articleId, UUID userId, String content) {
        this.articleId = articleId;
        this.userId = userId;
        this.content = content;
    }

    public static CreateCommentRequest create(UUID articleId, UUID userId, String content) {
        return CreateCommentRequest.builder()
                .articleId(articleId)
                .userId(userId)
                .content(content)
                .build();
    }
}
