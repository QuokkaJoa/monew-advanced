package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.*;

@Getter
@NoArgsConstructor
@ToString(exclude = {"comments", "interestMappings", "views"})
@Entity
@Table(name = "news_articles")
public class NewsArticle {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "news_articles_id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "published_date")
    private Timestamp publishedDate;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "view_count", columnDefinition = "BIGINT DEFAULT 0")
    private Long viewCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;


    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CommentsManagement> comments = new ArrayList<>();

    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<InterestNewsArticle> interestMappings = new HashSet<>();


    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ActivityDetail> views = new ArrayList<>();

    public NewsArticle(String sourceUrl, String title, Timestamp publishedDate, String summary, Long viewCount) {
        this.sourceUrl = sourceUrl;
        this.title = title;
        this.publishedDate = publishedDate;
        this.summary = summary;
        this.viewCount = viewCount;
    }
}
