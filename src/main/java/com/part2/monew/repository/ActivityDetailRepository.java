package com.part2.monew.repository;

import com.part2.monew.entity.ActivityDetail;
import com.part2.monew.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityDetailRepository extends JpaRepository<ActivityDetail, UUID> {

  List<ActivityDetail> findTop10ByUserAndNewsArticleIsNotNullOrderByViewedAtDesc(User user);
}
