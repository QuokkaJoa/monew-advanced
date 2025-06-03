package com.part2.monew.repository;

import com.part2.monew.entity.Interest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public interface InterestRepository extends JpaRepository<Interest, UUID> {

  @Query("SELECT DISTINCT i FROM Interest i LEFT JOIN i.interestKeywords ik " +
      "WHERE (:searchTerm IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(ik.keyword.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
      "AND ((:nameCursor IS NULL AND :createdAtCursor IS NULL) OR i.name > :nameCursor OR (i.name = :nameCursor AND i.createdAt > :createdAtCursor)) " +
      "ORDER BY i.name ASC, i.createdAt ASC")
  List<Interest> findByNameAsc(
      @Param("searchTerm") String searchTerm,
      @Param("nameCursor") String nameCursor,
      @Param("createdAtCursor") Timestamp createdAtCursor,
      Pageable pageable
  );

  @Query("SELECT DISTINCT i FROM Interest i LEFT JOIN i.interestKeywords ik " +
      "WHERE (:searchTerm IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(ik.keyword.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
      "AND ((:nameCursor IS NULL AND :createdAtCursor IS NULL) OR i.name < :nameCursor OR (i.name = :nameCursor AND i.createdAt < :createdAtCursor)) " +
      "ORDER BY i.name DESC, i.createdAt DESC")
  List<Interest> findByNameDesc(
      @Param("searchTerm") String searchTerm,
      @Param("nameCursor") String nameCursor,
      @Param("createdAtCursor") Timestamp createdAtCursor,
      Pageable pageable
  );


  @Query("SELECT DISTINCT i FROM Interest i LEFT JOIN i.interestKeywords ik " +
      "WHERE (:searchTerm IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(ik.keyword.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
      "AND ((:subscriberCountCursor IS NULL AND :createdAtCursor IS NULL) OR i.subscriberCount > :subscriberCountCursor OR (i.subscriberCount = :subscriberCountCursor AND i.createdAt > :createdAtCursor)) " +
      "ORDER BY i.subscriberCount ASC, i.createdAt ASC")
  List<Interest> findBySubscriberCountAsc(
      @Param("searchTerm") String searchTerm,
      @Param("subscriberCountCursor") Integer subscriberCountCursor,
      @Param("createdAtCursor") Timestamp createdAtCursor,
      Pageable pageable
  );


  @Query("SELECT DISTINCT i FROM Interest i LEFT JOIN i.interestKeywords ik " +
      "WHERE (:searchTerm IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(ik.keyword.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
      "AND ((:subscriberCountCursor IS NULL AND :createdAtCursor IS NULL) OR i.subscriberCount < :subscriberCountCursor OR (i.subscriberCount = :subscriberCountCursor AND i.createdAt < :createdAtCursor)) " +
      "ORDER BY i.subscriberCount DESC, i.createdAt DESC")
  List<Interest> findBySubscriberCountDesc(
      @Param("searchTerm") String searchTerm,
      @Param("subscriberCountCursor") Integer subscriberCountCursor,
      @Param("createdAtCursor") Timestamp createdAtCursor,
      Pageable pageable
  );


  @Query("SELECT COUNT(DISTINCT i.id) FROM Interest i LEFT JOIN i.interestKeywords ik " +
      "WHERE :searchTerm IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
      "OR LOWER(ik.keyword.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
  long countWithSearchTerm(@Param("searchTerm") String searchTerm);


  /**
   * 동일한 이름의 관심사가 존재하는지 확인합니다. (관심사 등록 시 정확한 이름 중복 검사 용도)
   * @param name 확인할 관심사 이름
   * @return 존재하면 true, 그렇지 않으면 false
   */
  boolean existsByName(String name);


  /**
   * 관심사 테이블에 조회해서 모든 이름들을 받아옵니다
   * @param ""
   * @return 관심사의 전체 이름
   */
  @Query("SELECT i.name FROM Interest i")
  List<String> findAllNames();
}
