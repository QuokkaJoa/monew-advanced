package com.part2.monew.service;

import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.request.CreateCommentRequest;
import com.part2.monew.dto.response.CommentResponse;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.entity.CommentsManagement;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.User;
import com.part2.monew.repository.CommentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional(readOnly = true)
class CommentServiceImplTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    @Transactional
    @DisplayName("기사 ID로 댓글 목록을 조회하고 CursorResponse로 반환한다.")
    void findCommentsByArticleId() {
        // given
        User user = new User("test@example.com", "pass123", "tester", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("http://url.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        Instant baseTime = Instant.parse("2025-06-01T00:00:00Z");

        CommentsManagement cm1 = CommentsManagement.create(user, article, "내용1", 0, Timestamp.from(baseTime.plus(1, ChronoUnit.HOURS)));
        CommentsManagement cm2 = CommentsManagement.create(user, article, "내용2", 0, Timestamp.from(baseTime.plus(2, ChronoUnit.HOURS)));
        CommentsManagement cm3 = CommentsManagement.create(user, article, "내용3", 0, Timestamp.from(baseTime.plus(3, ChronoUnit.HOURS)));
        CommentsManagement cm4 = CommentsManagement.create(user, article, "내용4", 0, Timestamp.from(baseTime.plus(4, ChronoUnit.HOURS)));
        CommentsManagement cm5 = CommentsManagement.create(user, article, "내용5", 0, Timestamp.from(baseTime.plus(5, ChronoUnit.HOURS)));
        CommentsManagement cm6 = CommentsManagement.create(user, article, "내용6", 0, Timestamp.from(baseTime.plus(6, ChronoUnit.HOURS)));

        em.persist(cm1);
        em.persist(cm2);
        em.persist(cm3);
        em.persist(cm4);
        em.persist(cm5);
        em.persist(cm6);

        em.flush();
        em.clear();

        CommentRequest request = CommentRequest.builder()
                .articleId(article.getId())
                .limit(5)
                .orderBy("createdAt")
                .direction("DESC")
                .build();

        // when
        CursorResponse response = commentService.findCommentsByArticleId(request);

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

        CreateCommentRequest comment = CreateCommentRequest.create( article.getId(), user.getId(), "내용");
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
        assertThatThrownBy( () -> commentService.create(comment))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("user with id " + userFailedId + " not found");
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

        CreateCommentRequest comment = CreateCommentRequest.create( article.getId(), user.getId(), "내용");
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

}