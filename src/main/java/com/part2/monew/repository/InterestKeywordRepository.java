package com.part2.monew.repository;

import com.part2.monew.entity.InterestKeyword;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

    
    @Query(value = """
        SELECT k.name 
        FROM interests_keywords ik
        JOIN interests i ON ik.interest_id = i.interest_id
        JOIN keywords k ON ik.keyword_id = k.keyword_id
        WHERE i.name = :interestName
        """, nativeQuery = true)
    List<String> findKeywordsByInterestName(@Param("interestName") String interestName);
    
   
    @Query(value = """
        SELECT i.name as interest_name, k.name as keyword_name
        FROM interests_keywords ik
        JOIN interests i ON ik.interest_id = i.interest_id
        JOIN keywords k ON ik.keyword_id = k.keyword_id
        ORDER BY i.name, k.name
        """, nativeQuery = true)
    List<Object[]> findAllInterestKeywordMappings();
} 