package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name="users")
public class User {
    @Id
    @UuidGenerator
    @Column(name = "user_id")
    private UUID id;

    private String nickname;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 30)
    private String password;

    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false,updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "user")
    private List<UserSubscriber> Usersubscribe;

    @OneToMany(mappedBy = "user")
    private List<CommentLike> commentLikes;

    @OneToMany(mappedBy = "user")
    private List<CommentsManagement> commentManagement;

    @OneToMany(mappedBy = "user")
    private List<ActivityDetail> activitiyDetail;

    @OneToMany(mappedBy = "user")
    private List<Notification> notification;

}
