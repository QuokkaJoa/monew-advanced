package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="users")
public class User {
    @Id
    @UuidGenerator
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "nickname")
    private String username;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 30)
    private String password;

    @Column(nullable = false)
    private boolean active = true;

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

    public User(String username, String email, String password, boolean active, Timestamp createdAt) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.active = active;
        this.createdAt = createdAt;
    }
}
