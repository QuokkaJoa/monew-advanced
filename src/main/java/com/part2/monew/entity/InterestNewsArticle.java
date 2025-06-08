package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "interests_news_articles")
public class InterestNewsArticle {
    @Id
    @UuidGenerator
    @Column(name = "interest_news_article_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interests_id", nullable = false)
    private Interest interest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_articles_id", nullable = false)
    private NewsArticle newsArticle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    public static InterestNewsArticle create(Interest interest, NewsArticle newsArticle) {
        return InterestNewsArticle.builder()
            .interest(interest)
            .newsArticle(newsArticle)
            .build();
    }
}
