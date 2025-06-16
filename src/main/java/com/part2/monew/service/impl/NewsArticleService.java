package com.part2.monew.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.part2.monew.dto.request.FilterDto;
import com.part2.monew.dto.request.RequestCursorDto;
import com.part2.monew.dto.response.NewsArticleResponseDto;
import com.part2.monew.dto.response.PaginatedResponseDto;
import com.part2.monew.entity.ActivityDetail;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.User;
import com.part2.monew.global.exception.article.ArticleDeleteFailedException;
import com.part2.monew.global.exception.article.ArticleNotFoundException;
import com.part2.monew.global.exception.article.ArticleRestoreFailedException;
import com.part2.monew.global.exception.article.ArticleSearchFailedException;
import com.part2.monew.global.exception.user.UserNotFoundException;
import com.part2.monew.mapper.NewsArticleMapper;
import com.part2.monew.repository.ActivityDetailRepository;
import com.part2.monew.repository.CommentRepository;
import com.part2.monew.repository.NewsArticleRepository;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.NewsBackupS3Manager;
import com.part2.monew.util.DateTimeUtil;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsArticleService {

    private static final Logger logger = LoggerFactory.getLogger(NewsArticleService.class);

    @Getter
    private final NewsArticleRepository newsArticleRepository;
    private final NewsArticleMapper newsArticleMapper;
    private final NewsBackupS3Manager newsBackupS3Manager;
    private final ActivityDetailRepository activityDetailRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ObjectMapper objectMapper;

    public NewsArticleService(NewsArticleRepository newsArticleRepository,
        NewsArticleMapper newsArticleMapper, NewsBackupS3Manager newsBackupS3Manager,
        ActivityDetailRepository activityDetailRepository, UserRepository userRepository,
        CommentRepository commentRepository) {
        this.newsArticleRepository = newsArticleRepository;
        this.newsArticleMapper = newsArticleMapper;
        this.newsBackupS3Manager = newsBackupS3Manager;
        this.activityDetailRepository = activityDetailRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Transactional(readOnly = true)
    public PaginatedResponseDto<NewsArticleResponseDto> getArticles(FilterDto filterDto,
        RequestCursorDto cursorDto, String userId) {
        logger.info("뉴스 기사 조회 요청 - 사용자 ID: {}, 필터: {}, 커서: {}", userId, filterDto, cursorDto);

        // limit이 0 이하인 경우 기본값 20
        int effectiveLimit = cursorDto.limit() > 0 ? cursorDto.limit() : 20;

        try {

            return switch (cursorDto.orderBy()) {
                case "commentCount" ->
                    getArticlesSortedByCommentCount(filterDto, cursorDto, effectiveLimit);
                case "viewCount" ->
                    getArticlesSortedByViewCount(filterDto, cursorDto, effectiveLimit);
                default -> getArticlesSortedByPublishDate(filterDto, cursorDto, effectiveLimit);
            };

        } catch (Exception e) {
            logger.error("뉴스 기사 검색 중 오류 발생", e);
            throw new ArticleSearchFailedException();
        }
    }

    private PaginatedResponseDto<NewsArticleResponseDto> getArticlesSortedByCommentCount(
        FilterDto filterDto, RequestCursorDto cursorDto, int effectiveLimit) {

        List<NewsArticle> articles = newsArticleRepository.findArticlesSortedByCommentCount(
            filterDto.keyword(), getFirstSource(filterDto.sourceIn()), filterDto.publishDateFrom(),
            filterDto.publishDateTo(), cursorDto.direction(), cursorDto.cursor(),
            effectiveLimit + 1);

        return buildPaginatedResponseWithoutCommentQuery(articles, cursorDto, effectiveLimit);
    }

    private PaginatedResponseDto<NewsArticleResponseDto> getArticlesSortedByViewCount(
        FilterDto filterDto, RequestCursorDto cursorDto, int effectiveLimit) {

        List<NewsArticle> articles = newsArticleRepository.findArticlesSortedByViewCount(
            filterDto.keyword(), getFirstSource(filterDto.sourceIn()), filterDto.publishDateFrom(),
            filterDto.publishDateTo(), cursorDto.direction(), cursorDto.cursor(),
            effectiveLimit + 1);

        return buildPaginatedResponse(articles, cursorDto, effectiveLimit);
    }

    private PaginatedResponseDto<NewsArticleResponseDto> getArticlesSortedByPublishDate(
        FilterDto filterDto, RequestCursorDto cursorDto, int effectiveLimit) {

        List<NewsArticle> articles = newsArticleRepository.findArticlesSortedByPublishDate(
            filterDto.keyword(), getFirstSource(filterDto.sourceIn()), filterDto.publishDateFrom(),
            filterDto.publishDateTo(), cursorDto.direction(), cursorDto.cursor(),
            effectiveLimit + 1);

        return buildPaginatedResponse(articles, cursorDto, effectiveLimit);
    }

    private PaginatedResponseDto<NewsArticleResponseDto> buildPaginatedResponseWithoutCommentQuery(
        List<NewsArticle> articles, RequestCursorDto cursorDto, int effectiveLimit) {

        // 커서 기반 페이징 처리
        boolean hasNext = articles.size() > effectiveLimit;
        String nextCursor = null;
        Timestamp nextAfter = null;
        Long nextCursorViewCount = null;

        if (hasNext) {
            articles = articles.subList(0, effectiveLimit);
        }

        List<UUID> articleIds = articles.stream().map(NewsArticle::getId)
            .collect(Collectors.toList());
        Map<UUID, Long> commentCountMap = new HashMap<>();

        if (!articleIds.isEmpty()) {
            List<Object[]> commentCounts = commentRepository.countActiveCommentsByArticleIds(
                articleIds);
            for (Object[] result : commentCounts) {
                UUID articleId = (UUID) result[0];
                Long count = ((Number) result[1]).longValue();
                commentCountMap.put(articleId, count);
            }
        }

        // 다음 커서 값 계산
        if (hasNext && !articles.isEmpty()) {
            NewsArticle lastArticle = articles.get(articles.size() - 1);
            Long actualCommentCount = commentCountMap.getOrDefault(lastArticle.getId(), 0L);
            nextCursor = String.valueOf(actualCommentCount);
            nextAfter = lastArticle.getPublishedDate();
        }

        Map<UUID, Boolean> viewedStatusMap = Collections.emptyMap();
        List<NewsArticleResponseDto> responseDtos = articles.stream().map(article -> {
            Long actualCommentCount = commentCountMap.getOrDefault(article.getId(), 0L);
            return newsArticleMapper.toDto(article,
                viewedStatusMap.getOrDefault(article.getId(), false), actualCommentCount);
        }).collect(Collectors.toList());

        return PaginatedResponseDto.<NewsArticleResponseDto>builder().content(responseDtos)
            .nextCursor(nextCursor).nextAfter(nextAfter).nextCursorViewCount(nextCursorViewCount)
            .size(responseDtos.size()).totalElements(responseDtos.size()).hasNext(hasNext)
            .build();
    }

    private PaginatedResponseDto<NewsArticleResponseDto> buildPaginatedResponse(
        List<NewsArticle> articles, RequestCursorDto cursorDto, int effectiveLimit) {

        boolean hasNext = articles.size() > effectiveLimit;
        String nextCursor = null;
        Timestamp nextAfter = null;
        Long nextCursorViewCount = null;

        if (hasNext) {
            articles = articles.subList(0, effectiveLimit);
        }

        // 다음 커서 값 계산
        if (hasNext && !articles.isEmpty()) {
            NewsArticle lastArticle = articles.get(articles.size() - 1);

            switch (cursorDto.orderBy()) {
                case "viewCount":
                    nextCursor = String.valueOf(lastArticle.getViewCount());
                    nextCursorViewCount = lastArticle.getViewCount();
                    break;
                case "publishDate":
                default:
                    nextCursor = lastArticle.getPublishedDate().toString();
            }

            nextAfter = lastArticle.getPublishedDate();
        }

        // 응답 DTO 변환 (엔티티의 commentCount 필드 사용)
        Map<UUID, Boolean> viewedStatusMap = Collections.emptyMap();
        List<NewsArticleResponseDto> responseDtos = articles.stream().map(article -> {
            Long commentCount = article.getCommentCount(); // 엔티티의 commentCount 필드 사용
            return newsArticleMapper.toDto(article,
                viewedStatusMap.getOrDefault(article.getId(), false), commentCount);
        }).collect(Collectors.toList());

        return PaginatedResponseDto.<NewsArticleResponseDto>builder().content(responseDtos)
            .nextCursor(nextCursor).nextAfter(nextAfter).nextCursorViewCount(nextCursorViewCount)
            .size(responseDtos.size()).totalElements(responseDtos.size()).hasNext(hasNext)
            .build();
    }

    public List<String> getNewsSources() {
        try {
            List<String> sources = newsArticleRepository.findDistinctSources();

            if (sources == null || sources.isEmpty()) {
                sources = Arrays.asList("chosun", "hankyung", "yonhapnewstv", "NAVER");
            } else {
                logger.info("DB에서 뉴스 소스 목록 조회: {}", sources);
            }

            return sources;
        } catch (Exception e) {
            logger.error("뉴스 소스 조회 중 오류 발생", e);
            return Arrays.asList("chosun", "hankyung", "yonhapnewstv", "NAVER");
        }
    }


    @Transactional
    public void softDeleteArticle(UUID articleId) {
        NewsArticle article = newsArticleRepository.findActiveById(articleId)
            .orElseThrow(ArticleNotFoundException::new);

        try {
            article.softDelete();
            newsArticleRepository.save(article);
            logger.info("뉴스 기사 논리 삭제 완료: {}", articleId);
        } catch (Exception e) {
            logger.error("뉴스 기사 논리 삭제 실패: {}", articleId, e);
            throw new ArticleDeleteFailedException();
        }
    }

    @Transactional
    public void hardDeleteArticle(UUID articleId) {
        NewsArticle article = newsArticleRepository.findById(articleId)
            .orElseThrow(ArticleNotFoundException::new);

        try {
            newsArticleRepository.delete(article);
        } catch (Exception e) {
            throw new ArticleDeleteFailedException();
        }
    }


    @Transactional
    public void restoreDataByDateRange(String fromDate, String toDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate from = LocalDate.parse(fromDate, formatter);
        LocalDate to = LocalDate.parse(toDate, formatter);

        logger.info("데이터 복구 요청: {} ~ {}", fromDate, toDate);

        List<NewsArticle> restoredArticles = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            String s3Key = newsBackupS3Manager.getBackupFileKey(date);
            try (InputStream backupStream = newsBackupS3Manager.downloadNewsBackup(s3Key)) {
                if (backupStream == null) {
                    logger.warn("{} 날짜의 백업 파일이 S3에 없습니다. Key: {}", date, s3Key);
                    continue;
                }

                List<NewsArticle> articlesFromBackup = objectMapper.readValue(backupStream,
                    new TypeReference<>() {
                    });

                if (articlesFromBackup != null && !articlesFromBackup.isEmpty()) {
                    logger.info("{} 날짜의 백업에서 {}개 기사 로드됨. Key: {}", date, articlesFromBackup.size(),
                        s3Key);
                    for (NewsArticle article : articlesFromBackup) {
                        // DB에 이미 존재하는지 확인
                        if (!newsArticleRepository.existsBySourceUrl(article.getSourceUrl())) {

                            NewsArticle newArticle = NewsArticle.builder()
                                .sourceIn(article.getSourceIn()).sourceUrl(article.getSourceUrl())
                                .title(article.getTitle()).publishedDate(article.getPublishedDate())
                                .summary(article.getSummary()).viewCount(article.getViewCount())
                                .commentCount(article.getCommentCount())
                                .isDeleted(article.getIsDeleted()).build();
                            newsArticleRepository.save(newArticle);
                            restoredArticles.add(newArticle);
                            logger.debug("복구된 기사 저장: {}", newArticle.getSourceUrl());
                        } else {
                            logger.debug("이미 존재하는 기사 (복구 건너뜀): {}", article.getSourceUrl());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("{} 날짜의 데이터 복구 중 오류 발생. Key: {}. Error: {}", date, s3Key,
                    e.getMessage(), e);
            }
        }

    }

    @Transactional
    public int restoreFromLatestBackup() {
        logger.info("최신 백업에서 삭제된 기사 복구 시작");

        try {
            String latestBackupKey = newsBackupS3Manager.getLatestBackupKey();
            int restoredCount = 0;

            try (InputStream backupStream = newsBackupS3Manager.downloadNewsBackup(
                latestBackupKey)) {
                if (backupStream == null) {
                    logger.warn("최신 백업 파일이 S3에 없습니다. Key: {}", latestBackupKey);
                    return 0;
                }

                List<NewsArticle> articlesFromBackup = objectMapper.readValue(backupStream,
                    new TypeReference<>() {
                    });

                if (articlesFromBackup != null && !articlesFromBackup.isEmpty()) {

                    for (NewsArticle article : articlesFromBackup) {
                        if (!newsArticleRepository.existsBySourceUrl(article.getSourceUrl())) {

                            NewsArticle newArticle = NewsArticle.builder()
                                .sourceIn(article.getSourceIn()).sourceUrl(article.getSourceUrl())
                                .title(article.getTitle()).publishedDate(article.getPublishedDate())
                                .summary(article.getSummary()).viewCount(article.getViewCount())
                                .commentCount(article.getCommentCount())
                                .isDeleted(false) // 복구된 기사는 활성 상태
                                .build();

                            newsArticleRepository.save(newArticle);
                            restoredCount++;
                        }
                    }
                }
            }

            logger.info("최신 백업 복구 완료: 총 {}개 기사 복구됨. Key: {}", restoredCount, latestBackupKey);
            return restoredCount;

        } catch (Exception e) {
            logger.error("최신 백업 복구 중 오류 발생: {}", e.getMessage(), e);
            throw new ArticleRestoreFailedException();
        }
    }


    public void backupDataByDate(LocalDate date) {
        logger.info("데이터 백업 시작: {}", date);

        Timestamp startOfDayTimestamp = DateTimeUtil.parseTimestamp(date.toString());
        Timestamp nextDayStartTimestamp = DateTimeUtil.parseTimestampAsNextDayStart(date.toString());

        List<NewsArticle> articlesToBackup = newsArticleRepository.findByIsDeletedFalseAndPublishedDateBetween(
            startOfDayTimestamp, nextDayStartTimestamp);

        if (articlesToBackup.isEmpty()) {
            logger.info("{} 날짜에 백업할 뉴스 기사가 없습니다.", date);
            return;
        }

        try {
            byte[] jsonData = objectMapper.writeValueAsBytes(articlesToBackup);
            String s3Key = newsBackupS3Manager.getBackupFileKey(date);
            newsBackupS3Manager.uploadNewsBackup(jsonData, s3Key);
            logger.info("데이터 백업 완료: {} 날짜의 {}개 기사. S3 Key: {}", date, articlesToBackup.size(),
                s3Key);
        } catch (Exception e) {
            logger.error("{} 날짜의 데이터 백업 중 오류 발생: {}", date, e.getMessage(), e);
            throw new RuntimeException(date + " 날짜 데이터 백업 실패", e);
        }
    }

    @Transactional
    public void incrementViewCount(UUID articleId, UUID userId) {
        NewsArticle article = newsArticleRepository.findActiveById(articleId)
            .orElseThrow(ArticleNotFoundException::new);

        User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("해당 사용자를 찾을 수 없습니다."));

        // 이미 조회한 기록이 있는지 확인
        boolean alreadyViewed = activityDetailRepository.existsByUserIdAndArticleId(userId,
            articleId);

        if (!alreadyViewed) {
            // 처음 보는 기사인 경우에만 조회수 증가
            article.incrementViewCount();
            newsArticleRepository.save(article);

            ActivityDetail activityDetail = ActivityDetail.builder().user(user).newsArticle(article)
                .viewedAt(new Timestamp(System.currentTimeMillis())).build();

            activityDetailRepository.save(activityDetail);

        } else {
            logger.info("이미 조회한 기사이므로 조회수 증가 안함");
        }
    }


    private String getFirstSource(List<String> sources) {
        return (sources != null && !sources.isEmpty()) ? sources.get(0) : null;
    }


}
