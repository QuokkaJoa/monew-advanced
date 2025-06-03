package com.part2.monew.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CommentRequest {
    private UUID articleId;

    private String orderBy;

    private String direction;

    private String cursor;

    private Timestamp after;

    private Integer limit;

    private UUID requestUserId;

    @Builder
    private CommentRequest(UUID articleId, String orderBy, String direction, String cursor, Timestamp after, Integer limit, UUID requestUserId) {
        this.articleId = articleId;
        this.orderBy = orderBy;
        this.direction = direction;
        this.cursor = cursor;
        this.after = after;
        this.limit = limit;
        this.requestUserId = requestUserId;
    }
}
