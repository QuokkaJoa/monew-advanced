package com.part2.monew.repository;

import com.part2.monew.entity.ActivityDetail;
import com.part2.monew.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityDetailRepository extends JpaRepository<ActivityDetail, UUID> {

  // 특정 사용자가 특정 기사를 조회한 기록이 있는지 확인
  @Query("SELECT COUNT(ad) > 0 FROM ActivityDetail ad WHERE ad.user.id = :userId AND ad.newsArticle.id = :articleId")
  boolean existsByUserIdAndArticleId(@Param("userId") UUID userId, @Param("articleId") UUID articleId);

  List<ActivityDetail> findTop10ByUserAndNewsArticleIsNotNullOrderByViewedAtDesc(User user);
}
