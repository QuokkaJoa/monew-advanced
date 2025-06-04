package com.part2.monew.repository;

import com.part2.monew.entity.CommentsManagement;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static com.part2.monew.entity.QCommentsManagement.commentsManagement;
import static com.part2.monew.entity.QNewsArticle.newsArticle;
import static com.part2.monew.entity.QUser.user;

public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public CommentRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<CommentsManagement> findCommentsByArticleId(UUID articleId, Timestamp after, int limit) {
        return queryFactory
                .selectFrom(commentsManagement)
                .join(commentsManagement.user, user).fetchJoin()
                .join(commentsManagement.newsArticle, newsArticle).fetchJoin()
                .where(
                        commentsManagement.newsArticle.id.eq(articleId),
                        commentsManagement.active.isTrue(),
                        ltCreatedAt(after)
                )
                .orderBy(commentsManagement.createdAt.desc())
                .limit(limit + 1)
                .fetch();
    }

    @Override
    public Long totalCount(UUID articleId) {
        return queryFactory
                .selectFrom(commentsManagement)
                .where(
                        commentsManagement.newsArticle.id.eq(articleId),
                        commentsManagement.active.isTrue()
                ).fetchCount();
    }

    private BooleanExpression ltCreatedAt(Timestamp after) {
        return after != null ? commentsManagement.createdAt.lt(after) : null;
    }

}
