package com.part2.monew.repository;

import com.part2.monew.entity.CommentLike;
import com.part2.monew.entity.CommentsManagement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {
    List<CommentLike> findAllByCommentsManagement(CommentsManagement commentsManagement);

    Optional<CommentLike> findByCommentsManagement_IdAndUser_Id(UUID id, UUID userId);
}
