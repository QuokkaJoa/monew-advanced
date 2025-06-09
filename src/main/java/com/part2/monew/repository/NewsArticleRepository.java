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
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID>, NewsArticleRepositoryCustom {

    // ID로 활성 기사 조회
    @Query("SELECT n FROM NewsArticle n WHERE n.id = :id AND n.isDeleted = false")
    Optional<NewsArticle> findActiveById(@Param("id") UUID id);

    // URL 중복 체크
    boolean existsBySourceUrl(String sourceUrl);

    // 뉴스 소스 목록 조회
    @Query("SELECT DISTINCT n.sourceIn FROM NewsArticle n WHERE n.isDeleted = false AND n.sourceIn IS NOT NULL")
    List<String> findDistinctSources();

    // 백업용 날짜 범위 조회
    List<NewsArticle> findByIsDeletedFalseAndPublishedDateBetween(Timestamp startDate, Timestamp endDate);


}
