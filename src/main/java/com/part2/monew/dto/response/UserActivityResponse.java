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

    private List<UserSubscriberResponse> subscriptions;
    private List<CommentActivityDto> comments;
    private List<CommentLikeActivityDto> commentLikes;
    private List<NewsArticleSummaryDto> articleViews;

}

