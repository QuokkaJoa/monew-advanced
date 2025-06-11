package com.part2.monew.service;

import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.response.CommentLikeResponse;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.entity.CommentsManagement;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.User;
import com.part2.monew.mapper.InterestMapper;
import com.part2.monew.repository.CommentLikeRepository;
import com.part2.monew.global.exception.ErrorCode;
import com.part2.monew.global.exception.comment.CommentLikeDuplication;
import com.part2.monew.global.exception.comment.CommentUnlikeDuplication;
import com.part2.monew.global.exception.user.UserNotFoundException;
import com.part2.monew.repository.CommentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(readOnly = true)
class CommentServiceImplTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @MockitoBean
    private InterestMapper interestMapper;

    @Test
    @Transactional
    @DisplayName("기사 ID로 댓글 목록을 조회하고 CursorResponse로 반환한다.")
    void findCommentsByArticleId() {
        // given: 초기 데이터 세팅
        User user = new User(
                "test@example.com",
                "pass123",
                "tester",
                true,
                Timestamp.from(Instant.now())
        );
        em.persist(user);

        NewsArticle article = new NewsArticle(
                "http://url.com",
                "제목",
                Timestamp.from(Instant.now()),
                "요약",
                0L
        );
        em.persist(article);

        Instant baseTime = Instant.parse("2025-06-01T00:00:00Z");
        for (int i = 1; i <= 6; i++) {
            CommentsManagement cm = CommentsManagement.create(
                    user, article, "내용" + i, 0,
                    Timestamp.from(baseTime.plus(i, ChronoUnit.HOURS))
            );
            em.persist(cm);
        }

        // 서비스가 자신의 트랜잭션을 열도록, 영속성 컨텍스트만 초기화
        em.flush();
        em.clear();

        CommentRequest request = CommentRequest.builder()
                .articleId(article.getId())
                .limit(5)
                .orderBy("createdAt")
                .direction("DESC")
                .build();

        // when
        CursorResponse response = commentService.findCommentsByArticleId(request, user.getId());

        // then
        assertThat(response.getContent()).hasSize(5);
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getTotalElements()).isEqualTo(6);
    }


    @DisplayName("댓글을 저장한다.")
    @Test
    @Transactional
    void create() {
        // given
        User user = new User("tester", "test@example.com", "pass123", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        CreateCommentRequest comment = CreateCommentRequest.create(article.getId(), user.getId(), "내용");
        em.flush();
        em.clear();

        // when
        CommentResponse saved = commentService.create(comment);

        // then
        CommentsManagement fetched = commentRepository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Saved comment not found"));

        assertThat(fetched)
                .extracting("user.id", "newsArticle.id", "content", "likeCount", "active")
                .containsExactly(user.getId(), article.getId(), "내용", 0, true);

    }

    @DisplayName("댓글을 저장실패")
    @Test
    @Transactional
    void createFailed() {
        // given
        User user = new User("tester", "test@example.com", "pass123", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        UUID userFailedId = UUID.randomUUID();

        CreateCommentRequest comment = CreateCommentRequest.create(article.getId(), userFailedId, "내용");
        em.flush();
        em.clear();
        // when // then
        assertThatThrownBy(() -> commentService.create(comment))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("댓글을 수정한다.")
    @Test
    @Transactional
    void update() {
        // given
        User user = new User("tester", "test@example.com", "pass123", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        CreateCommentRequest comment = CreateCommentRequest.create(article.getId(), user.getId(), "내용");
        CommentResponse saved = commentService.create(comment);
        em.flush();
        em.clear();

        // when
        commentService.update(saved.getId(), "새로운 내용");

        // then
        CommentsManagement fetched = commentRepository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Saved comment not found"));

        assertThat(fetched)
                .extracting("user.id", "newsArticle.id", "content", "likeCount", "active")
                .containsExactly(user.getId(), article.getId(), "새로운 내용", 0, true);


    }


    @DisplayName("댓글에 좋아요를 누른다.")
    @Test
    @Transactional
    void likeComment() {
        // given
        User user = new User("tester", "test@example.com", "pass123", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        CommentsManagement comment = CommentsManagement.create(user, article, "댓글", 0);

        commentRepository.save(comment);

        // when
        CommentLikeResponse response = commentService.likeComment(comment.getId(), user.getId());

        // then
        assertThat(response)
                .extracting(
                        "id",
                        "likeBy",
                        "createdAt",
                        "commentId",
                        "articleId",
                        "commentUserId",
                        "commentUserNickname",
                        "content",
                        "likeCount",
                        "commentCreatedAt")
                .containsExactly(
                        response.getId(),
                        response.getLikeBy(),
                        response.getCreatedAt(),
                        comment.getId(),
                        article.getId(),
                        comment.getUser().getId(),
                        comment.getUser().getNickname(),
                        comment.getContent(),
                        1,
                        comment.getCreatedAt());
    }

    @DisplayName("댓글 좋아요가 중복 호출되면 예외가 발생한다.")
    @Test
    @Transactional
    void likeDuplicatonComment() {
        // given
        User user = new User("tester", "test@example.com", "pass123", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        CommentsManagement comment = CommentsManagement.create(user, article, "댓글", 0);

        commentRepository.save(comment);

        CommentLikeResponse response = commentService.likeComment(comment.getId(), user.getId());

        // when then
        assertThatThrownBy(() -> commentService.likeComment(comment.getId(), user.getId()))
                .isInstanceOf(CommentLikeDuplication.class)
                .hasMessage(ErrorCode.COMMENT_LIKE_DUPLICATION.getMessage());

    }


    @DisplayName("댓글에 좋아요를 한번더 누른다.")
    @Test
    @Transactional
    void unlikeComment() {
        // given
        User user = new User("tester", "test@example.com", "pass123", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        CommentsManagement comment = CommentsManagement.create(user, article, "댓글", 0);

        commentRepository.save(comment);

        CommentLikeResponse response = commentService.likeComment(comment.getId(), user.getId());

        // when
        commentService.unlikeComment(comment.getId(), user.getId());
        em.flush();
        em.clear();

        // then
        CommentsManagement updated = commentRepository.findById(comment.getId())
                .orElseThrow(() -> new AssertionError("댓글이 DB에 존재하지 않습니다."));

        assertThat(updated.getLikeCount()).isEqualTo(0);
    }


    @DisplayName("댓글 좋아요 취소가 중복 호출되면 예외가 발생한다.")
    @Test
    @Transactional
    void unlikeDuplicatonComment() {
        // given
        User user = new User("tester", "test@example.com", "pass123", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        CommentsManagement comment = CommentsManagement.create(user, article, "댓글", 0);

        commentRepository.save(comment);

        // when then
        assertThatThrownBy(() -> commentService.unlikeComment(comment.getId(), user.getId()))
                .isInstanceOf(CommentUnlikeDuplication.class)
                .hasMessage(ErrorCode.COMMENT_UNLIKE_DUPLICATION.getMessage());

    }

    @DisplayName("댓글을 삭제한다.")
    @Test
    @Transactional
    void deleteComment() {
        // given
        User user = new User("tester", "test@example.com", "pass123", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        CommentsManagement comment = CommentsManagement.create(user, article, "댓글", 0);

        commentRepository.save(comment);

        // when
        commentService.deleteComment(comment.getId());

        // then
        CommentsManagement updated = commentRepository.findById(comment.getId())
                .orElseThrow(() -> new AssertionError("댓글이 DB에 존재하지 않습니다."));

        assertThat(updated.isActive()).isFalse();
    }
}



