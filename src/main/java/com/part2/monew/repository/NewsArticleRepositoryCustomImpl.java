package com.part2.monew.repository;

import static com.part2.monew.entity.QCommentsManagement.commentsManagement;
import static com.part2.monew.entity.QNewsArticle.newsArticle;

import com.part2.monew.entity.NewsArticle;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NewsArticleRepositoryCustomImpl implements NewsArticleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public NewsArticleRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<NewsArticle> findArticlesSortedByCommentCount(String keyword, String sourceIn,
        Timestamp publishDateFrom, Timestamp publishDateTo, String direction, String cursor,
        int limit) {
        
        return findArticlesWithFiltersAndSorting(keyword, sourceIn, publishDateFrom, 
            publishDateTo, "commentCount", direction, cursor, limit);
    }

    @Override
    public List<NewsArticle> findArticlesWithFiltersAndSorting(String keyword, String sourceIn,
        Timestamp publishDateFrom, Timestamp publishDateTo, String orderBy, String direction,
        String cursor, int limit) {

        BooleanBuilder whereCondition = buildBaseCondition(keyword, sourceIn, publishDateFrom,
            publishDateTo);

        // 커서 조건 추가
        if (cursor != null && !cursor.trim().isEmpty()) {
            whereCondition.and(buildCursorCondition(orderBy, direction, cursor));
        }

        // 정렬 조건
        OrderSpecifier<?>[] orderSpecifiers = buildOrderSpecifiers(orderBy, direction);

        return queryFactory.selectFrom(newsArticle).where(whereCondition).orderBy(orderSpecifiers)
            .limit(limit).fetch();
    }

    // 복합 커서를 지원하는 새로운 메서드 추가
    public List<NewsArticle> findArticlesWithFiltersAndSortingComposite(String keyword,
        String sourceIn, Timestamp publishDateFrom, Timestamp publishDateTo, String orderBy,
        String direction, String cursor, Timestamp cursorPublishedDate, int limit) {

        BooleanBuilder whereCondition = buildBaseCondition(keyword, sourceIn, publishDateFrom,
            publishDateTo);

        // 복합 커서 조건 추가
        if (cursor != null && !cursor.trim().isEmpty()) {
            BooleanExpression cursorCondition = buildCompositeCursorCondition(orderBy, direction,
                cursor, cursorPublishedDate);
            if (cursorCondition != null) {
                whereCondition.and(cursorCondition);
            }
        }

        // 정렬 조건
        OrderSpecifier<?>[] orderSpecifiers = buildOrderSpecifiers(orderBy, direction);

        return queryFactory.selectFrom(newsArticle).where(whereCondition).orderBy(orderSpecifiers)
            .limit(limit).fetch();
    }

    @Override
    public List<NewsArticle> findArticlesSortedByViewCount(String keyword, String sourceIn,
        Timestamp publishDateFrom, Timestamp publishDateTo, String direction, String cursor,
        int limit) {
        
        return findArticlesWithFiltersAndSorting(keyword, sourceIn, publishDateFrom, 
            publishDateTo, "viewCount", direction, cursor, limit);
    }

    @Override
    public List<NewsArticle> findArticlesSortedByPublishDate(String keyword, String sourceIn,
        Timestamp publishDateFrom, Timestamp publishDateTo, String direction, String cursor,
        int limit) {
        
        return findArticlesWithFiltersAndSorting(keyword, sourceIn, publishDateFrom, 
            publishDateTo, "publishDate", direction, cursor, limit);
    }

    @Override
    public List<String> findExistingSourceUrls(List<String> sourceUrls) {
        return queryFactory.select(newsArticle.sourceUrl).from(newsArticle)
            .where(newsArticle.sourceUrl.in(sourceUrls)).fetch();
    }

    private BooleanBuilder buildBaseCondition(String keyword, String sourceIn,
        Timestamp publishDateFrom, Timestamp publishDateTo) {

        BooleanBuilder whereCondition = new BooleanBuilder();

        // 기본 조건
        whereCondition.and(newsArticle.isDeleted.isFalse());

        // 키워드 검색
        if (keyword != null && !keyword.trim().isEmpty()) {
            whereCondition.and(newsArticle.title.containsIgnoreCase(keyword)
                .or(newsArticle.summary.containsIgnoreCase(keyword)));
        }

        // 소스 필터
        if (sourceIn != null && !sourceIn.trim().isEmpty()) {
            whereCondition.and(newsArticle.sourceIn.eq(sourceIn));
        }

        if (publishDateFrom != null) {
            whereCondition.and(newsArticle.publishedDate.goe(publishDateFrom));
        }
        if (publishDateTo != null) {
            whereCondition.and(newsArticle.publishedDate.lt(publishDateTo));  // < 조건으로 인덱스 최적화
        }

        return whereCondition;
    }

    private BooleanExpression buildCursorCondition(String orderBy, String direction,
        String cursor) {
        try {
            return switch (orderBy) {
                case "publishDate" -> {
                    Timestamp cursorDate = Timestamp.valueOf(cursor);
                    yield "DESC".equalsIgnoreCase(direction) ? newsArticle.publishedDate.lt(
                        cursorDate) : newsArticle.publishedDate.gt(cursorDate);
                }
                case "viewCount" -> {
                    Long cursorViewCount = Long.parseLong(cursor);
                    yield "DESC".equalsIgnoreCase(direction) ? newsArticle.viewCount.lt(
                        cursorViewCount) : newsArticle.viewCount.gt(cursorViewCount);
                }
                case "commentCount" -> {
                    Long cursorCommentCount = Long.parseLong(cursor);
                    yield "DESC".equalsIgnoreCase(direction) ? newsArticle.commentCount.lt(
                        cursorCommentCount) : newsArticle.commentCount.gt(cursorCommentCount);
                }
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Invalid cursor format: {}", cursor, e);
            return null;
        }
    }

    // 복합 커서 조건을 사용하는 새로운 메서드 추가
    private BooleanExpression buildCompositeCursorCondition(String orderBy, String direction,
        String cursor, Timestamp cursorPublishedDate) {
        try {
            switch (orderBy) {
                case "publishDate":
                    Timestamp cursorDate = Timestamp.valueOf(cursor);
                    return "DESC".equalsIgnoreCase(direction) ? newsArticle.publishedDate.lt(
                        cursorDate) : newsArticle.publishedDate.gt(cursorDate);

                case "viewCount":
                    Long cursorViewCount = Long.parseLong(cursor);
                    BooleanExpression viewCountCondition;
                    BooleanExpression publishDateCondition;

                    if ("DESC".equalsIgnoreCase(direction)) {
                        // viewCount가 더 작거나, 같으면서 publishedDate가 더 작은 경우
                        viewCountCondition = newsArticle.viewCount.lt(cursorViewCount);
                        publishDateCondition = newsArticle.viewCount.eq(cursorViewCount).and(
                            cursorPublishedDate != null ? newsArticle.publishedDate.lt(
                                cursorPublishedDate) : null);
                    } else {
                        // viewCount가 더 크거나, 같으면서 publishedDate가 더 큰 경우
                        viewCountCondition = newsArticle.viewCount.gt(cursorViewCount);
                        publishDateCondition = newsArticle.viewCount.eq(cursorViewCount).and(
                            cursorPublishedDate != null ? newsArticle.publishedDate.gt(
                                cursorPublishedDate) : null);
                    }

                    return publishDateCondition != null ? viewCountCondition.or(
                        publishDateCondition) : viewCountCondition;

                case "commentCount":
                    Long cursorCommentCount = Long.parseLong(cursor);
                    BooleanExpression commentCountCondition;
                    BooleanExpression commentPublishDateCondition;

                    if ("DESC".equalsIgnoreCase(direction)) {
                        commentCountCondition = newsArticle.commentCount.lt(cursorCommentCount);
                        commentPublishDateCondition = newsArticle.commentCount.eq(
                            cursorCommentCount).and(
                            cursorPublishedDate != null ? newsArticle.publishedDate.lt(
                                cursorPublishedDate) : null);
                    } else {
                        commentCountCondition = newsArticle.commentCount.gt(cursorCommentCount);
                        commentPublishDateCondition = newsArticle.commentCount.eq(
                            cursorCommentCount).and(
                            cursorPublishedDate != null ? newsArticle.publishedDate.gt(
                                cursorPublishedDate) : null);
                    }

                    return commentPublishDateCondition != null ? commentCountCondition.or(
                        commentPublishDateCondition) : commentCountCondition;

                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("Invalid cursor format: {}", cursor, e);
            return null;
        }
    }
    // orderby
    private OrderSpecifier<?>[] buildOrderSpecifiers(String orderBy, String direction) {
        Order order = "ASC".equalsIgnoreCase(direction) ? Order.ASC : Order.DESC;

        return switch (orderBy) {
            case "publishDate" ->
                new OrderSpecifier[]{new OrderSpecifier<>(order, newsArticle.publishedDate)};
            case "viewCount" ->
                new OrderSpecifier[]{new OrderSpecifier<>(order, newsArticle.viewCount),
                    newsArticle.publishedDate.desc()};
            case "commentCount" ->
                new OrderSpecifier[]{new OrderSpecifier<>(order, newsArticle.commentCount),
                    newsArticle.publishedDate.desc()};
            default -> new OrderSpecifier[]{newsArticle.publishedDate.desc()};
        };
    }
}
