package com.part2.monew.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.Timestamp;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "news_articles")
public class NewsArticle {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "news_article_id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    //추가 예)네이버 기사 조선 기사
    @Column(name = "source_in", length = 100)
    private String sourceIn;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "published_date")
    private Timestamp publishedDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "view_counts", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "comment_counts", columnDefinition = "BIGINT DEFAULT 0")
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
    @JsonIgnore
    private List<CommentsManagement> comments = new ArrayList<>();

    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<InterestNewsArticle> interestMappings = new HashSet<>();


    @OneToMany(mappedBy = "newsArticle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ActivityDetail> views = new ArrayList<>();

    public NewsArticle(String sourceUrl, String title, Timestamp publishedDate, String summary, Long viewCount) {
        this.isDeleted = false;
        this.sourceUrl = sourceUrl;
        this.title = title;
        this.publishedDate = publishedDate;
        this.summary = summary;
        this.viewCount = viewCount;
    }

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
