package com.part2.monew.repository;

import com.part2.monew.entity.NewsArticle;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    // ID로 활성 기사 조회
    @Query("SELECT n FROM NewsArticle n WHERE n.id = :id AND n.isDeleted = false")
    Optional<NewsArticle> findActiveById(@Param("id") UUID id);

    // 통합 검색 및 정렬 ( 관심사,출처,날짜 조합/ 정렬: 날짜,댓글수,조회수 중 1개)
    @Query(value = """
        SELECT * FROM news_articles n 
        WHERE n.is_deleted = false 
        AND (CAST(:keyword AS TEXT) IS NULL OR :keyword = '' OR 
             LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')) OR 
             LOWER(n.summary) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
        AND (CAST(:sourceIn AS TEXT) IS NULL OR :sourceIn = '' OR n.source_in = CAST(:sourceIn AS TEXT))
        AND (CAST(:publishDateFrom AS TIMESTAMP) IS NULL OR n.published_date >= CAST(:publishDateFrom AS TIMESTAMP))
        AND (CAST(:publishDateTo AS TIMESTAMP) IS NULL OR n.published_date <= CAST(:publishDateTo AS TIMESTAMP))
        AND (
            CAST(:cursor AS TEXT) IS NULL OR :cursor = '' OR
            (:orderBy = 'publishDate' AND :direction = 'DESC' AND n.published_date < CAST(:cursor AS TIMESTAMP)) OR
            (:orderBy = 'publishDate' AND :direction = 'ASC' AND n.published_date > CAST(:cursor AS TIMESTAMP)) OR
            (:orderBy = 'viewCount' AND :direction = 'DESC' AND n.view_count < CAST(:cursor AS BIGINT)) OR
            (:orderBy = 'viewCount' AND :direction = 'ASC' AND n.view_count > CAST(:cursor AS BIGINT)) OR
            (:orderBy = 'commentCount' AND :direction = 'DESC' AND n.comment_count < CAST(:cursor AS BIGINT)) OR
            (:orderBy = 'commentCount' AND :direction = 'ASC' AND n.comment_count > CAST(:cursor AS BIGINT))
        )
        ORDER BY 
        CASE WHEN :orderBy = 'publishDate' AND :direction = 'DESC' THEN n.published_date END DESC,
        CASE WHEN :orderBy = 'publishDate' AND :direction = 'ASC' THEN n.published_date END ASC,
        CASE WHEN :orderBy = 'viewCount' AND :direction = 'DESC' THEN n.view_count END DESC,
        CASE WHEN :orderBy = 'viewCount' AND :direction = 'ASC' THEN n.view_count END ASC,
        CASE WHEN :orderBy = 'commentCount' AND :direction = 'DESC' THEN n.comment_count END DESC,
        CASE WHEN :orderBy = 'commentCount' AND :direction = 'ASC' THEN n.comment_count END ASC,
        n.published_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<NewsArticle> findArticlesWithFiltersAndSorting(
        @Param("keyword") String keyword,
        @Param("sourceIn") String sourceIn,
        @Param("publishDateFrom") Timestamp publishDateFrom,
        @Param("publishDateTo") Timestamp publishDateTo,
        @Param("orderBy") String orderBy,
        @Param("direction") String direction,
        @Param("cursor") String cursor,
        @Param("limit") int limit
    );

    // URL 중복 체크
    boolean existsBySourceUrl(String sourceUrl);

    // 뉴스 소스 목록 조회
    @Query("SELECT DISTINCT n.sourceIn FROM NewsArticle n WHERE n.isDeleted = false AND n.sourceIn IS NOT NULL")
    List<String> findDistinctSources();

    // 백업용 날짜 범위 조회
    List<NewsArticle> findByIsDeletedFalseAndPublishedDateBetween(Timestamp startDate, Timestamp endDate);

    // URL 목록 존재 여부 확인
    @Query(value = "SELECT source_url FROM news_articles WHERE source_url IN (:sourceUrls)", nativeQuery = true)
    List<String> findExistingSourceUrls(@Param("sourceUrls") List<String> sourceUrls);

    // 댓글 수 정렬 전용 쿼리 (실제 댓글 수로 정렬)
    @Query(value = """
        SELECT n.*
        FROM news_articles n 
        LEFT JOIN (
            SELECT cm.news_article_id, COUNT(cm.comment_management_id) as actual_comment_count
            FROM comments_managements cm 
            WHERE cm.active = true 
            GROUP BY cm.news_article_id
        ) comment_counts ON n.news_articles_id = comment_counts.news_article_id
        WHERE n.is_deleted = false 
        AND (CAST(:keyword AS TEXT) IS NULL OR :keyword = '' OR 
             LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')) OR 
             LOWER(n.summary) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
        AND (CAST(:sourceIn AS TEXT) IS NULL OR :sourceIn = '' OR n.source_in = CAST(:sourceIn AS TEXT))
        AND (CAST(:publishDateFrom AS TIMESTAMP) IS NULL OR n.published_date >= CAST(:publishDateFrom AS TIMESTAMP))
        AND (CAST(:publishDateTo AS TIMESTAMP) IS NULL OR n.published_date <= CAST(:publishDateTo AS TIMESTAMP))
        AND (
            CAST(:cursor AS TEXT) IS NULL OR :cursor = '' OR
            (:direction = 'DESC' AND COALESCE(comment_counts.actual_comment_count, 0) < CAST(:cursor AS BIGINT)) OR
            (:direction = 'ASC' AND COALESCE(comment_counts.actual_comment_count, 0) > CAST(:cursor AS BIGINT))
        )
        ORDER BY 
            CASE WHEN :direction = 'DESC' THEN COALESCE(comment_counts.actual_comment_count, 0) END DESC,
            CASE WHEN :direction = 'ASC' THEN COALESCE(comment_counts.actual_comment_count, 0) END ASC,
            n.published_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<NewsArticle> findArticlesSortedByCommentCount(
        @Param("keyword") String keyword,
        @Param("sourceIn") String sourceIn,
        @Param("publishDateFrom") Timestamp publishDateFrom,
        @Param("publishDateTo") Timestamp publishDateTo,
        @Param("direction") String direction,
        @Param("cursor") String cursor,
        @Param("limit") int limit
    );

    // 조회 수 정렬 전용 쿼리
    @Query(value = """
        SELECT * FROM news_articles n 
        WHERE n.is_deleted = false 
        AND (CAST(:keyword AS TEXT) IS NULL OR :keyword = '' OR 
             LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')) OR 
             LOWER(n.summary) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
        AND (CAST(:sourceIn AS TEXT) IS NULL OR :sourceIn = '' OR n.source_in = CAST(:sourceIn AS TEXT))
        AND (CAST(:publishDateFrom AS TIMESTAMP) IS NULL OR n.published_date >= CAST(:publishDateFrom AS TIMESTAMP))
        AND (CAST(:publishDateTo AS TIMESTAMP) IS NULL OR n.published_date <= CAST(:publishDateTo AS TIMESTAMP))
        AND (
            CAST(:cursor AS TEXT) IS NULL OR :cursor = '' OR
            (:direction = 'DESC' AND n.view_count < CAST(:cursor AS BIGINT)) OR
            (:direction = 'ASC' AND n.view_count > CAST(:cursor AS BIGINT))
        )
        ORDER BY 
            CASE WHEN :direction = 'DESC' THEN n.view_count END DESC,
            CASE WHEN :direction = 'ASC' THEN n.view_count END ASC,
            n.published_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<NewsArticle> findArticlesSortedByViewCount(
        @Param("keyword") String keyword,
        @Param("sourceIn") String sourceIn,
        @Param("publishDateFrom") Timestamp publishDateFrom,
        @Param("publishDateTo") Timestamp publishDateTo,
        @Param("direction") String direction,
        @Param("cursor") String cursor,
        @Param("limit") int limit
    );

    // 날짜 정렬 전용 쿼리
    @Query(value = """
        SELECT * FROM news_articles n 
        WHERE n.is_deleted = false 
        AND (CAST(:keyword AS TEXT) IS NULL OR :keyword = '' OR 
             LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')) OR 
             LOWER(n.summary) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
        AND (CAST(:sourceIn AS TEXT) IS NULL OR :sourceIn = '' OR n.source_in = CAST(:sourceIn AS TEXT))
        AND (CAST(:publishDateFrom AS TIMESTAMP) IS NULL OR n.published_date >= CAST(:publishDateFrom AS TIMESTAMP))
        AND (CAST(:publishDateTo AS TIMESTAMP) IS NULL OR n.published_date <= CAST(:publishDateTo AS TIMESTAMP))
        AND (
            CAST(:cursor AS TEXT) IS NULL OR :cursor = '' OR
            (:direction = 'DESC' AND n.published_date < CAST(:cursor AS TIMESTAMP)) OR
            (:direction = 'ASC' AND n.published_date > CAST(:cursor AS TIMESTAMP))
        )
        ORDER BY 
            CASE WHEN :direction = 'DESC' THEN n.published_date END DESC,
            CASE WHEN :direction = 'ASC' THEN n.published_date END ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<NewsArticle> findArticlesSortedByPublishDate(
        @Param("keyword") String keyword,
        @Param("sourceIn") String sourceIn,
        @Param("publishDateFrom") Timestamp publishDateFrom,
        @Param("publishDateTo") Timestamp publishDateTo,
        @Param("direction") String direction,
        @Param("cursor") String cursor,
        @Param("limit") int limit
    );
}
