package com.part2.monew.repository;

import com.part2.monew.entity.CommentsManagement;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
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
    public List<CommentsManagement> findCommentsByArticleId(UUID articleId, Timestamp after, int limit, String orderBy, String direction) {
        return queryFactory
                .selectFrom(commentsManagement)
                .join(commentsManagement.user, user).fetchJoin()
                .join(commentsManagement.newsArticle, newsArticle).fetchJoin()
                .where(
                        commentsManagement.newsArticle.id.eq(articleId),
                        commentsManagement.active.isTrue(),
                        ltCreatedAt(after)
                )
                .orderBy(getOrderSpecifier(orderBy, direction))
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

    private OrderSpecifier<?> getOrderSpecifier(String orderBy, String direction) {
        Order order = "ASC".equalsIgnoreCase(direction) ? Order.ASC : Order.DESC;
        
        if (orderBy == null || orderBy.trim().isEmpty()) {
            orderBy = "createdAt"; // 기본값
        }
        
        switch (orderBy.toLowerCase()) {
            case "createdat":
            case "created_at":
                return new OrderSpecifier<>(order, commentsManagement.createdAt);
            case "likecount":
            case "like_count":
                return new OrderSpecifier<>(order, commentsManagement.likeCount);
            default:
                return new OrderSpecifier<>(Order.DESC, commentsManagement.createdAt);
        }
    }

    @Override
    public List<CommentsManagement> findTop10RecentCommentsByUserId(UUID userId) {
        return queryFactory
            .selectFrom(commentsManagement)
            .join(commentsManagement.user, user).fetchJoin()
            .join(commentsManagement.newsArticle, newsArticle).fetchJoin()
            .where(
                commentsManagement.user.id.eq(userId),
                commentsManagement.active.isTrue()
            )
            .orderBy(commentsManagement.createdAt.desc())
            .limit(10)
            .fetch();
    }

}
