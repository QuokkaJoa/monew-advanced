package com.part2.monew.service.impl;

import com.part2.monew.dto.response.*;
import com.part2.monew.entity.User;
import com.part2.monew.global.exception.user.UserNotFoundException;
import com.part2.monew.repository.*;
import com.part2.monew.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {

  private final UserRepository userRepository;
  private final UserSubscriberRepository userSubscriberRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ActivityDetailRepository activityDetailRepository;

  @Override
  @Transactional(readOnly = true)
  public UserActivityResponse getUserActivity(UUID userId) {
    User user = userRepository.findByIdAndActiveTrue(userId)
        .orElseThrow(UserNotFoundException::new);

    List<UserSubscriberResponse> subscriptions = userSubscriberRepository.findByUser(user).stream()
        .map(UserSubscriberResponse::of)
        .toList();

    List<CommentActivityDto> comments = commentRepository.findTop10RecentCommentsByUserId(userId).stream()
        .map(CommentActivityDto::of)
        .toList();

    List<CommentLikeActivityDto> commentLikes = commentLikeRepository
        .findTop10ByUser_IdOrderByCreatedAtDesc(userId).stream()
        .map(cl -> CommentLikeActivityDto.of(cl))
        .toList();

    List<NewsArticleSummaryDto> articleViews = activityDetailRepository
        .findTop10ByUserAndNewsArticleIsNotNullOrderByViewedAtDesc(user).stream()
        .map(NewsArticleSummaryDto::of)
        .toList();

    return UserActivityResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .nickname(user.getUsername())
        .createdAt(user.getCreatedAt())
        .subscriptions(subscriptions)
        .comments(comments)
        .commentLikes(commentLikes)
        .articleViews(articleViews)
        .build();
  }
}
