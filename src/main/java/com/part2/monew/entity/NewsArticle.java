package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "news_articles")
public class NewsArticle {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "news_articles_id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    //추가 예)네이버 기사 조선 기사
    @Column(name = "sourceIn", length = 100)
    private String sourceIn;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "published_date")
    private Timestamp publishedDate;

    @Lob
    @Column(nullable = false, length = 10000)
    private String summary;

    @Column(name = "view_count", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "comment_count", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long commentCount = 0L;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;


    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CommentsManagement> comments = new ArrayList<>();

    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<InterestNewsArticle> interestMappings = new HashSet<>();


    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ActivityDetail> views = new ArrayList<>();

    public void softDelete() {
        this.isDeleted = true;
    }

    public boolean isDeleted() {
        return this.isDeleted != null && this.isDeleted;
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0L : this.viewCount) + 1L;
    }

    public void incrementCommentCount() {
        this.commentCount = (this.commentCount == null ? 0L : this.commentCount) + 1L;
    }

    public void decrementCommentCount() {
        if (this.commentCount != null && this.commentCount > 0) {
            this.commentCount--;
        }
    }
}
