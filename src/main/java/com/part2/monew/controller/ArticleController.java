package com.part2.monew.controller;


import com.part2.monew.dto.request.FilterDto;
import com.part2.monew.dto.request.RequestCursorDto;
import com.part2.monew.dto.response.NewsArticleResponseDto;
import com.part2.monew.dto.response.PaginatedResponseDto;
import com.part2.monew.dto.response.RestoreResultDto;
import com.part2.monew.service.impl.NewsArticleService;
import com.part2.monew.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
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
@Slf4j
public class ArticleController {

    private final NewsArticleService newsArticleService;

    public ArticleController(NewsArticleService newsArticleService) {
        this.newsArticleService = newsArticleService;
    }

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
        @RequestHeader(value = "Monew-Request-User-ID", required = false) String userId) {
        
        try {
            // 날짜 파싱
            Timestamp publishDateFromTs = DateTimeUtil.parseTimestamp(publishDateFrom);
            Timestamp publishDateToTs = DateTimeUtil.parseTimestampAsNextDayStart(publishDateTo);
            Timestamp afterTs = DateTimeUtil.parseTimestamp(after);
            
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

    @PostMapping("/{articleId}/article-views")
    public ResponseEntity<Void> incrementViewCountAlternative(
        @PathVariable String articleId,
        @RequestHeader(value = "Monew-Request-User-ID", required = false) String userId) {
        
        try {
            UUID articleUuid = UUID.fromString(articleId);
            UUID userUuid = UUID.fromString(userId);
            newsArticleService.incrementViewCount(articleUuid, userUuid);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/sources")
    public ResponseEntity<List<String>> getSources() {
        List<String> sources = newsArticleService.getNewsSources();
        return ResponseEntity.ok(sources);
    }

    @GetMapping("/restore")
    public ResponseEntity<RestoreResultDto> restoreArticles(
        @RequestParam(value = "fromDate", required = false) String fromDate,
        @RequestParam(value = "toDate", required = false) String toDate) {
        
        if (fromDate != null && toDate != null) {
            newsArticleService.restoreDataByDateRange(fromDate, toDate);
            return ResponseEntity.ok(RestoreResultDto.builder()
                .restoreDate(new Timestamp(System.currentTimeMillis()))
                .restoredArticleIds(List.of())
                .restoredArticleCount(0L)
                .build());
        } else {
            int restoredCount = newsArticleService.restoreFromLatestBackup();
            return ResponseEntity.ok(RestoreResultDto.builder()
                .restoreDate(new Timestamp(System.currentTimeMillis()))
                .restoredArticleIds(List.of())
                .restoredArticleCount((long) restoredCount)
                .build());
        }
    }

    @PostMapping("/backup")
    public ResponseEntity<String> backupArticles(
        @RequestParam(value = "date", required = false) String date) {
        
        try {
            if (date != null) {
                java.time.LocalDate backupDate = java.time.LocalDate.parse(date);
                newsArticleService.backupDataByDate(backupDate);
                return ResponseEntity.ok("백업 완료: " + date);
            } else {
                // 어제 데이터 백업
                java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
                newsArticleService.backupDataByDate(yesterday);
                return ResponseEntity.ok("어제 데이터 백업 완료: " + yesterday);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("백업 실패: " + e.getMessage());
        }
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

    // 기사 물리 삭제
    @DeleteMapping("/{articleId}/hard")
    public ResponseEntity<Void> deleteArticleHard(@PathVariable String articleId) {
        try {
            UUID uuid = UUID.fromString(articleId);
            newsArticleService.hardDeleteArticle(uuid);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }


}
