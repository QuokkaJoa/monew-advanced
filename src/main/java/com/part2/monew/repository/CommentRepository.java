package com.part2.monew.repository;

import com.part2.monew.entity.CommentsManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<CommentsManagement, UUID>, CommentRepositoryCustom {

}
