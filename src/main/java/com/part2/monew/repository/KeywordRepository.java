package com.part2.monew.repository;

import com.part2.monew.entity.Keyword;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, UUID> {

  Optional<Keyword> findByName(String name);
}
