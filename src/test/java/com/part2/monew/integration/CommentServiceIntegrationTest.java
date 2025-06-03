package com.part2.monew.integration;

import com.part2.monew.dto.request.CommentRequest;
import com.part2.monew.dto.response.CursorResponse;
import com.part2.monew.entity.CommentsManagement;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.User;
import com.part2.monew.service.CommentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CommentServiceIntegrationTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private CommentService commentService;

    @Test
    @DisplayName("댓글 목록을 커서 기반으로 조회한다")
    void testFindCommentsByArticleIdWithoutForLoop() {
        // given
        User user = new User("test@example.com", "pw", "nickname", true, Timestamp.from(Instant.now()));
        em.persist(user);

        NewsArticle article = new NewsArticle("https://foo.com", "제목", Timestamp.from(Instant.now()), "요약", 0L);
        em.persist(article);

        em.persist(CommentsManagement.create(user, article, "내용1", 0, Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS))));
        em.persist(CommentsManagement.create(user, article, "내용2", 0, Timestamp.from(Instant.now().plus(2, ChronoUnit.DAYS))));
        em.persist(CommentsManagement.create(user, article, "내용3", 0, Timestamp.from(Instant.now().plus(3, ChronoUnit.DAYS))));
        em.persist(CommentsManagement.create(user, article, "내용4", 0, Timestamp.from(Instant.now().plus(4, ChronoUnit.DAYS))));
        em.persist(CommentsManagement.create(user, article, "내용5", 0, Timestamp.from(Instant.now().plus(5, ChronoUnit.DAYS))));
        em.persist(CommentsManagement.create(user, article, "내용6", 0, Timestamp.from(Instant.now().plus(6, ChronoUnit.DAYS))));

        em.flush();
        em.clear();

        CommentRequest request = CommentRequest.builder()
                .articleId(article.getId())
                .orderBy("createdAt")
                .direction("ASC")
                .limit(5)
                .build();

        // when
        CursorResponse response = commentService.findCommentsByArticleId(request);

        // then
        assertThat(response.getContent()).hasSize(5);
        assertThat(response.getHasNext()).isTrue();
        assertThat(response.getTotalElements()).isEqualTo(6);
    }
}
