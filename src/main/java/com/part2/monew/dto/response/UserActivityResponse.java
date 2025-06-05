package com.part2.monew.dto.response;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserActivityResponse {
    private UUID id;
    private String email;
    private String nickname;
    private Timestamp createdAt;

    private List<UserSubscriptionActivityResponse> subscriptions;
    private List<UserCommentActivityDto> comments;
    private List<UserCommentLikeActivityDto> commentLikes;
    private List<UserArticleViewsActivityDto> articleViews;

}

