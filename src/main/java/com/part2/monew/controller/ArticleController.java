package com.part2.monew.controller;


import com.part2.monew.dto.request.FilterDto;
import com.part2.monew.dto.request.RequestCursorDto;
import com.part2.monew.dto.response.NewsArticleResponseDto;
import com.part2.monew.dto.response.PaginatedResponseDto;
import com.part2.monew.service.impl.NewsArticleService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final NewsArticleService newsArticleService;

    public ArticleController(NewsArticleService newsArticleService) {
        this.newsArticleService = newsArticleService;
    }

    // 메인 검색 API (관심사, 출처, 날짜 필터 + 정렬 + 커서 기반 페이징)
    @GetMapping
    public ResponseEntity<PaginatedResponseDto<NewsArticleResponseDto>> getArticles(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "interestId", required = false) String interestId,
        @RequestParam(value = "sourceIn", required = false) List<String> sourceIn,
        @RequestParam(value = "publishDateFrom", required = false) String publishDateFrom,
        @RequestParam(value = "publishDateTo", required = false) String publishDateTo,
        @RequestParam(value = "orderBy", defaultValue = "publishDate") String orderBy,
        @RequestParam(value = "direction", defaultValue = "DESC") String direction,
        @RequestParam(value = "cursor", required = false) String cursor,
        @RequestParam(value = "after", required = false) String after,
        @RequestParam(value = "limit", defaultValue = "20") Integer limit,
        @RequestHeader(value = "Monew-Request-User-ID") String userId) {
        
        try {
            // 날짜 파싱
            Timestamp publishDateFromTs = parseTimestamp(publishDateFrom);
            Timestamp publishDateToTs = parseTimestamp(publishDateTo);
            Timestamp afterTs = parseTimestamp(after);
            
            // interestId를 UUID로 변환
            UUID interestUuid = null;
            if (interestId != null && !interestId.trim().isEmpty()) {
                try {
                    interestUuid = UUID.fromString(interestId);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }
            
         
            FilterDto filterDto = new FilterDto(
                keyword, 
                interestUuid,
                sourceIn, 
                publishDateFromTs, 
                publishDateToTs
            );
            
            RequestCursorDto cursorDto = new RequestCursorDto(
                orderBy, 
                direction, 
                cursor, 
                afterTs,
                null, // nextCursorViewCount는 내부에서 계산
                limit
            );
            
            PaginatedResponseDto<NewsArticleResponseDto> result = newsArticleService.getArticles(
                filterDto, cursorDto, userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 조회수 증가 (기존 경로)
    @PostMapping("/{articleId}/views")
    public ResponseEntity<Void> incrementViewCount(
        @PathVariable String articleId,
        @RequestHeader(value = "Monew-Request-User-ID") String userId) {
        
        try {
            UUID articleUuid = UUID.fromString(articleId);
            UUID userUuid = UUID.fromString(userId);
            newsArticleService.incrementViewCount(articleUuid, userUuid);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 조회수 증가 (새로운 경로)
    @PostMapping("/{articleId}/article-views")
    public ResponseEntity<Void> incrementViewCountAlternative(
        @PathVariable String articleId,
        @RequestHeader(value = "Monew-Request-User-ID") String userId) {
        
        try {
            UUID articleUuid = UUID.fromString(articleId);
            UUID userUuid = UUID.fromString(userId);
            newsArticleService.incrementViewCount(articleUuid, userUuid);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 뉴스 소스 목록 조회
    @GetMapping("/sources")
    public ResponseEntity<List<String>> getSources() {
        List<String> sources = newsArticleService.getNewsSources();
        return ResponseEntity.ok(sources);
    }

    // 기사 삭제
    @DeleteMapping("/{articleId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable String articleId) {
        try {
            UUID uuid = UUID.fromString(articleId);
            newsArticleService.softDeleteArticle(uuid);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Timestamp parseTimestamp(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // ISO 8601 형식 (타임존 포함) 시도: 2025-06-05T01:40:00.000+00:00
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString);
            return Timestamp.from(offsetDateTime.toInstant());
        } catch (DateTimeParseException e1) {
            try {
                // ISO_INSTANT 형식 시도: 2025-06-05T01:40:00.000Z
                Instant instant = Instant.parse(dateTimeString);
                return Timestamp.from(instant);
            } catch (DateTimeParseException e2) {
                try {
                    // LocalDateTime 형식 시도: 2025-06-05T01:40:00
                    LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return Timestamp.valueOf(localDateTime);
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Invalid date format: " + dateTimeString, e3);
                }
            }
        }
    }
}
