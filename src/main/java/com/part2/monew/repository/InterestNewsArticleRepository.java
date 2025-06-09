package com.part2.monew.repository;

import com.part2.monew.entity.InterestNewsArticle;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestNewsArticleRepository extends JpaRepository<InterestNewsArticle, UUID> {
    


     // 뉴스 기사와 관심사 간의 매핑이 이미 존재하는지 확인
    @Query(value = """
        SELECT COUNT(*) > 0 
        FROM interests_news_articles ina
        WHERE ina.news_articles_id = :newsArticleId AND ina.interests_id = :interestId
        """, nativeQuery = true)
    boolean existsByNewsArticleIdAndInterestId(@Param("newsArticleId") UUID newsArticleId, @Param("interestId") UUID interestId);
} 