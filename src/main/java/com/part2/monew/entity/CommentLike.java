package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="comments_like")
public class CommentLike {
    @Id
    @UuidGenerator
    @Column(name = "commentLike_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_management_id")
    private CommentsManagement commentsManagement;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;


    @Builder
    private CommentLike(User user, CommentsManagement commentsManagement) {
        this.user = user;
        this.commentsManagement = commentsManagement;
    }

    public static CommentLike create(User user, CommentsManagement commentsManagement) {
        return CommentLike.builder()
                .user(user)
                .commentsManagement(commentsManagement)
                .build();

    }
}
