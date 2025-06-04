package com.part2.monew.repository;

import com.part2.monew.entity.User;
import com.part2.monew.entity.UserSubscriber;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriberRepository extends JpaRepository<UserSubscriber, UUID> {

  List<UserSubscriber> findByUser(User user);
}
