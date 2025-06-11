package com.part2.monew.service;

import com.part2.monew.dto.response.*;
import com.part2.monew.entity.*;
import com.part2.monew.global.exception.user.UserNotFoundException;
import com.part2.monew.repository.*;
import com.part2.monew.service.impl.UserActivityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class UserActivityServiceImplTest {

  @InjectMocks
  private UserActivityServiceImpl userActivityService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserSubscriberRepository userSubscriberRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private CommentLikeRepository commentLikeRepository;

  @Mock
  private ActivityDetailRepository activityDetailRepository;

  private UUID userId;
  private User mockUser;
  private String testEmail;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    userId = UUID.randomUUID();
    testEmail = "user_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

    mockUser = new User();
    mockUser.setId(userId);
    mockUser.setEmail(testEmail);
    mockUser.setNickname("tester");
    mockUser.setActive(true);
    mockUser.setCreatedAt(Timestamp.from(Instant.now()));
  }

  @Test
  @DisplayName("정상적으로 사용자 활동 내역을 반환한다 - 구독, 댓글, 좋아요, 기사 포함")
  void getUserActivity_success() {
    // mock 구독
    Keyword k1 = new Keyword();
    k1.setName("기온");

    Keyword k2 = new Keyword();
    k2.setName("강수량");

    Interest interest = new Interest();
    interest.setId(UUID.randomUUID());
    interest.setName("날씨");
    interest.setSubscriberCount(42);

    InterestKeyword ik1 = new InterestKeyword();
    ik1.setKeyword(k1);
    ik1.setInterest(interest);

    InterestKeyword ik2 = new InterestKeyword();
    ik2.setKeyword(k2);
    ik2.setInterest(interest);

    interest.setInterestKeywords(List.of(ik1, ik2));

    UserSubscriber subscriber = new UserSubscriber();
    subscriber.setId(UUID.randomUUID());
    subscriber.setUser(mockUser);
    subscriber.setInterest(interest);
    subscriber.setCreatedAt(Timestamp.from(Instant.now()));

    // mock 댓글
    NewsArticle article = new NewsArticle();
    article.setId(UUID.randomUUID());
    article.setTitle("날씨 기사");

    CommentsManagement comment = new CommentsManagement();
    comment.setId(UUID.randomUUID());
    comment.setUser(mockUser);
    comment.setNewsArticle(article);
    comment.setContent("맑음입니다");
    comment.setLikeCount(3);
    comment.setActive(true);
    comment.setCreatedAt(Timestamp.from(Instant.now()));

    // mock 좋아요
    CommentLike commentLike = new CommentLike();
    commentLike.setId(UUID.randomUUID());
    commentLike.setUser(mockUser);
    commentLike.setCommentsManagement(comment);
    commentLike.setCreatedAt(Timestamp.from(Instant.now()));

    // mock 기사 보기
    ActivityDetail articleView = new ActivityDetail();
    articleView.setId(UUID.randomUUID());
    articleView.setUser(mockUser);
    articleView.setNewsArticle(article);
    articleView.setViewedAt(Timestamp.from(Instant.now()));

    // given
    when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(mockUser));
    when(userSubscriberRepository.findInterestsByUser(mockUser)).thenReturn(List.of(interest));
    when(commentRepository.findTop10RecentCommentsByUserId(userId)).thenReturn(List.of(comment));
    when(commentLikeRepository.findTop10ByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of(commentLike));
    when(activityDetailRepository.findRecentViewedArticlesByUser(mockUser)).thenReturn(List.of(articleView));

    // when
    UserActivityResponse response = userActivityService.getUserActivity(userId);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(userId);
    assertThat(response.getEmail()).isEqualTo(testEmail);
    assertThat(response.getNickname()).isEqualTo("tester");

    // 구독
    assertThat(response.getSubscriptions()).hasSize(1);
    UserSubscriptionActivityResponse sub = response.getSubscriptions().get(0);
    assertThat(sub.getInterestName()).isEqualTo("날씨");
    assertThat(sub.getInterestKeywords()).containsExactlyInAnyOrder("기온", "강수량");

    // 댓글
    assertThat(response.getComments()).hasSize(1);
    UserCommentActivityDto commentDto = response.getComments().get(0);
    assertThat(commentDto.getContent()).isEqualTo("맑음입니다");
    assertThat(commentDto.getLikeCount()).isEqualTo(3);

    // 좋아요
    assertThat(response.getCommentLikes()).hasSize(1);
    UserCommentLikeActivityDto likeDto = response.getCommentLikes().get(0);
    assertThat(likeDto.getCommentContent()).isEqualTo("맑음입니다");

    // 기사 보기
    assertThat(response.getArticleViews()).hasSize(1);
    UserArticleViewsActivityDto viewDto = response.getArticleViews().get(0);
    assertThat(viewDto.getArticleTitle()).isEqualTo("날씨 기사");

    // verify
    verify(userRepository).findByIdAndActiveTrue(userId);
    verify(userSubscriberRepository).findInterestsByUser(mockUser);
    verify(commentRepository).findTop10RecentCommentsByUserId(userId);
    verify(commentLikeRepository).findTop10ByUser_IdOrderByCreatedAtDesc(userId);
    verify(activityDetailRepository).findRecentViewedArticlesByUser(mockUser);
  }

  @Test
  @DisplayName("예외 발생 (사용자 존재 x)")
  void getUserActivity_userNotFound() {
    when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userActivityService.getUserActivity(userId))
        .isInstanceOf(UserNotFoundException.class);

    verify(userRepository).findByIdAndActiveTrue(userId);
    verifyNoMoreInteractions(userSubscriberRepository, commentRepository, commentLikeRepository, activityDetailRepository);
  }
}
