package com.part2.monew.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.part2.monew.dto.request.FilterDto;
import com.part2.monew.dto.request.RequestCursorDto;
import com.part2.monew.dto.response.NewsArticleResponseDto;
import com.part2.monew.dto.response.PaginatedResponseDto;
import com.part2.monew.dto.response.RestoreResultDto;
import com.part2.monew.entity.ActivityDetail;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.User;
import com.part2.monew.global.exception.article.ArticleDeleteFailedException;
import com.part2.monew.global.exception.article.ArticleNotFoundException;
import com.part2.monew.global.exception.article.ArticleSearchFailedException;
import com.part2.monew.global.exception.user.UserNotFoundException;
import com.part2.monew.mapper.NewsArticleMapper;
import com.part2.monew.repository.ActivityDetailRepository;
import com.part2.monew.repository.CommentRepository;
import com.part2.monew.repository.NewsArticleRepository;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.NewsBackupS3Manager;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.springframework.scheduling.annotation.Scheduled;
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

        List<NewsArticle> articles;

        try {
            // 커서 기반 페이징을 위해 limit+1로 조회
            int fetchLimit = cursorDto.limit() + 1;
            
            logger.info("검색 시작 - keyword: {}, source: {}, orderBy: {}, direction: {}, cursor: {}",
                filterDto.keyword(), getFirstSource(filterDto.sourceIn()), 
                cursorDto.orderBy(), cursorDto.direction(), cursorDto.cursor());

            logger.info("정렬 파라미터 확인 - orderBy: '{}', direction: '{}', orderBy=='commentCount': {}", 
                cursorDto.orderBy(), cursorDto.direction(), "commentCount".equals(cursorDto.orderBy()));

            // 정렬 조건에 따라 적절한 쿼리 호출
            switch (cursorDto.orderBy()) {
                case "commentCount":
                    logger.info("댓글 수 정렬 쿼리 호출: direction={}", cursorDto.direction());
                    articles = newsArticleRepository.findArticlesSortedByCommentCount(
                        filterDto.keyword(),
                        getFirstSource(filterDto.sourceIn()),
                        filterDto.publishDateFrom(),
                        filterDto.publishDateTo(),
                        cursorDto.direction(),
                        cursorDto.cursor(),
                        fetchLimit
                    );
                    break;
                case "viewCount":
                    logger.info("조회 수 정렬 쿼리 호출: direction={}", cursorDto.direction());
                    articles = newsArticleRepository.findArticlesSortedByViewCount(
                        filterDto.keyword(),
                        getFirstSource(filterDto.sourceIn()),
                        filterDto.publishDateFrom(),
                        filterDto.publishDateTo(),
                        cursorDto.direction(),
                        cursorDto.cursor(),
                        fetchLimit
                    );
                    break;
                case "publishDate":
                default:
                    logger.info("날짜 정렬 쿼리 호출: direction={}", cursorDto.direction());
                    articles = newsArticleRepository.findArticlesSortedByPublishDate(
                        filterDto.keyword(),
                        getFirstSource(filterDto.sourceIn()),
                        filterDto.publishDateFrom(),
                        filterDto.publishDateTo(),
                        cursorDto.direction(),
                        cursorDto.cursor(),
                        fetchLimit
                    );
                    break;
            }

            logger.info("검색 완료 - 결과: {}개", articles.size());

        } catch (Exception e) {
            logger.error("뉴스 기사 검색 중 오류 발생", e);
            throw new ArticleSearchFailedException();
        }

        // 커서 기반 페이징 처리
        boolean hasNext = articles.size() > cursorDto.limit();
        String nextCursor = null;
        Timestamp nextAfter = null;
        Long nextCursorViewCount = null;

        // hasNext가 true면 마지막 요소는 다음 페이지 표시용이므로 제거
        if (hasNext) {
            articles = articles.subList(0, cursorDto.limit());
        }

        // 응답 DTO 변환을 위해 먼저 모든 기사의 실제 댓글 수를 한 번에 조회 (N+1 문제 해결)
        List<UUID> articleIds = articles.stream().map(NewsArticle::getId).collect(Collectors.toList());
        Map<UUID, Long> commentCountMap = new HashMap<>();
        
        if (!articleIds.isEmpty()) {
            List<Object[]> commentCounts = commentRepository.countActiveCommentsByArticleIds(articleIds);
            for (Object[] result : commentCounts) {
                UUID articleId = (UUID) result[0];
                Long count = ((Number) result[1]).longValue();
                commentCountMap.put(articleId, count);
            }
        }

        // 다음 커서 값 계산
        if (hasNext && !articles.isEmpty()) {
            NewsArticle lastArticle = articles.get(articles.size() - 1);
            
            switch (cursorDto.orderBy()) {
                case "publishDate":
                    nextCursor = lastArticle.getPublishedDate().toString();
                    break;
                case "viewCount":
                    nextCursor = String.valueOf(lastArticle.getViewCount());
                    nextCursorViewCount = lastArticle.getViewCount();
                    break;
                case "commentCount":
                    // 실제 댓글 수를 Map에서 가져와서 커서로 사용
                    Long actualCommentCount = commentCountMap.getOrDefault(lastArticle.getId(), 0L);
                    nextCursor = String.valueOf(actualCommentCount);
                    break;
                default:
                    nextCursor = lastArticle.getPublishedDate().toString();
            }
            
            nextAfter = lastArticle.getPublishedDate();
        }

        // 응답 DTO 변환 (실제 댓글 수 포함)
        Map<UUID, Boolean> viewedStatusMap = Collections.emptyMap();
        List<NewsArticleResponseDto> responseDtos = articles.stream().map(article -> {
            // 실제 댓글 수를 Map에서 가져오기 (단일 조회로 이미 모든 댓글 수 로드됨)
            Long actualCommentCount = commentCountMap.getOrDefault(article.getId(), 0L);
            return newsArticleMapper.toDto(article,
                viewedStatusMap.getOrDefault(article.getId(), false), actualCommentCount);
        }).collect(Collectors.toList());

        return PaginatedResponseDto.<NewsArticleResponseDto>builder()
            .content(responseDtos)
            .nextCursor(nextCursor)
            .nextAfter(nextAfter)
            .nextCursorViewCount(nextCursorViewCount)
            .size(responseDtos.size())
            .totalElements((long) responseDtos.size()) // 커서 기반에서는 정확한 전체 개수 계산이 어려움
            .hasNext(hasNext)
            .build();
    }

    public List<String> getNewsSources() {
        try {
            // 실제 데이터베이스에서 distinct source 값들을 조회
            List<String> sources = newsArticleRepository.findDistinctSources();

            // 빈 리스트이거나 null인 경우 기본값 제공
            if (sources == null || sources.isEmpty()) {
                sources = Arrays.asList("chosun", "hankyung", "yonhapnewstv", "NAVER");
                logger.warn("DB에서 뉴스 소스를 찾을 수 없어 기본값 사용: {}", sources);
            } else {
                logger.info("DB에서 뉴스 소스 목록 조회: {}", sources);
            }

            return sources;
        } catch (Exception e) {
            logger.error("뉴스 소스 조회 중 오류 발생", e);
            // 오류 발생시 기본값 반환
            List<String> defaultSources = Arrays.asList("chosun", "hankyung", "yonhapnewstv",
                "NAVER");
            return defaultSources;
        }
    }

    public NewsArticle getArticleById(UUID articleId) {
        return newsArticleRepository.findActiveById(articleId)
            .orElseThrow(() -> new ArticleNotFoundException());
    }


    @Transactional
    public void softDeleteArticle(UUID articleId) {
        NewsArticle article = newsArticleRepository.findActiveById(articleId)
            .orElseThrow(() -> new ArticleNotFoundException());

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
            .orElseThrow(() -> new ArticleNotFoundException());

        try {
            newsArticleRepository.delete(article);
            logger.info("뉴스 기사 물리 삭제 완료: {}", articleId);
        } catch (Exception e) {
            logger.error("뉴스 기사 물리 삭제 실패: {}", articleId, e);
            throw new ArticleDeleteFailedException();
        }
    }


    public boolean existsActiveArticle(UUID articleId) {
        return newsArticleRepository.findActiveById(articleId).isPresent();
    }


    @Transactional
    public List<RestoreResultDto> restoreArticles(Timestamp from, Timestamp to) {
        logger.info("뉴스 기사 복구 요청: {} ~ {}", from, to);

        List<RestoreResultDto> restoreResults = new ArrayList<>();
        List<UUID> allRestoredIds = new ArrayList<>();

        LocalDate fromDate = from.toLocalDateTime().toLocalDate();
        LocalDate toDate = to.toLocalDateTime().toLocalDate();

        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            String s3Key = newsBackupS3Manager.getBackupFileKey(date);
            List<UUID> dailyRestoredIds = new ArrayList<>();

            try (InputStream backupStream = newsBackupS3Manager.downloadNewsBackup(s3Key)) {
                if (backupStream == null) {
                    logger.warn("{} 날짜의 백업 파일이 S3에 없습니다. Key: {}", date, s3Key);
                    continue;
                }

                List<NewsArticle> articlesFromBackup = objectMapper.readValue(backupStream,
                    new TypeReference<List<NewsArticle>>() {
                    });

                if (articlesFromBackup != null && !articlesFromBackup.isEmpty()) {
                    logger.info("{} 날짜의 백업에서 {}개 기사 로드됨", date, articlesFromBackup.size());

                    for (NewsArticle article : articlesFromBackup) {
                        // 날짜 범위 필터링
                        if (article.getPublishedDate() != null) {
                            Timestamp articleTimestamp = article.getPublishedDate();
                            if (articleTimestamp.before(from) || articleTimestamp.after(to)) {
                                continue;
                            }
                        }

                        // DB에 이미 존재하는지 확인 (sourceUrl 기준)
                        if (!newsArticleRepository.existsBySourceUrl(article.getSourceUrl())) {
                            NewsArticle newArticle = NewsArticle.builder()
                                .sourceIn(article.getSourceIn()).sourceUrl(article.getSourceUrl())
                                .title(article.getTitle()).publishedDate(article.getPublishedDate())
                                .summary(article.getSummary()).viewCount(article.getViewCount())
                                .commentCount(article.getCommentCount())
                                .isDeleted(false) // 복구된 기사는 활성 상태
                                .build();

                            NewsArticle savedArticle = newsArticleRepository.save(newArticle);
                            dailyRestoredIds.add(savedArticle.getId());
                            allRestoredIds.add(savedArticle.getId());
                        }
                    }
                }

                // 일일 복구 결과 추가 (복구된 기사가 있는 경우에만)
                if (!dailyRestoredIds.isEmpty()) {
                    RestoreResultDto dailyResult = RestoreResultDto.builder()
                        .restoreDate(new Timestamp(System.currentTimeMillis()))
                        .restoredArticleIds(dailyRestoredIds)
                        .restoredArticleCount((long) dailyRestoredIds.size()).build();
                    restoreResults.add(dailyResult);
                }

            } catch (Exception e) {
                logger.error("{} 날짜의 백업 복구 중 오류 발생", date, e);
            }
        }

        logger.info("뉴스 복구 완료. 총 {}개 기사 복구됨", allRestoredIds.size());
        return restoreResults;
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
                    new TypeReference<List<NewsArticle>>() {
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

        logger.info("데이터 복구 완료: 총 {}개 기사 복구됨. 기간: {} ~ {}", restoredArticles.size(), fromDate,
            toDate);
    }


    public void backupDataByDate(LocalDate date) {
        logger.info("데이터 백업 시작: {}", date);

        LocalDateTime startOfDayLocalDateTime = date.atStartOfDay();
        LocalDateTime endOfDayLocalDateTime = date.atTime(LocalTime.MAX);

        Timestamp startOfDayTimestamp = Timestamp.valueOf(startOfDayLocalDateTime);
        Timestamp endOfDayTimestamp = Timestamp.valueOf(endOfDayLocalDateTime);

        List<NewsArticle> articlesToBackup = newsArticleRepository.findByIsDeletedFalseAndPublishedDateBetween(
            startOfDayTimestamp, endOfDayTimestamp);

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
            .orElseThrow(() -> new ArticleNotFoundException());

        User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("해당 사용자를 찾을 수 없습니다."));

        // 이미 조회한 기록이 있는지 확인
        boolean alreadyViewed = activityDetailRepository.existsByUserIdAndArticleId(userId,
            articleId);

        if (!alreadyViewed) {
            // 처음 보는 기사인 경우에만 조회수 증가
            article.incrementViewCount();
            newsArticleRepository.save(article);

            // 활동 기록 저장
            ActivityDetail activityDetail = ActivityDetail.builder().user(user).newsArticle(article)
                .viewedAt(new Timestamp(System.currentTimeMillis())).build();

            activityDetailRepository.save(activityDetail);

            logger.info("뉴스 기사 조회수 증가 및 활동 기록 저장: {} (현재 조회수: {})", articleId,
                article.getViewCount());
        } else {
            logger.info("이미 조회한 기사이므로 조회수 증가 안함: {} (사용자: {})", articleId, userId);
        }
    }

    private boolean isSimpleKeywordSearch(FilterDto filterDto) {
        return filterDto.keyword() != null && filterDto.interestId() == null && (
            filterDto.sourceIn() == null || filterDto.sourceIn().isEmpty())
            && filterDto.publishDateFrom() == null && filterDto.publishDateTo() == null;
    }


    private String getFirstSource(List<String> sources) {
        return (sources != null && !sources.isEmpty()) ? sources.get(0) : null;
    }

    private UUID parseCursorId(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        return UUID.fromString(cursor);
    }

    private List<NewsArticle> limitResults(List<NewsArticle> articles, int limit) {
        if (articles.isEmpty()) {
            return articles;
        }

        int actualLimit = Math.min(limit, articles.size());
        return articles.subList(0, actualLimit);
    }

    @Scheduled(cron = "0 0 0 * * ?") // 매일 정각
    public void executeDailyNewsBackupBatch() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            backupDataByDate(yesterday);
        } catch (Exception e) {
            logger.error("일일 뉴스 백업 배치 실행 중 오류 발생: {}", e.getMessage(), e);

        }
    }

    public PaginatedResponseDto<NewsArticleResponseDto> getArticlesByParams(String keyword,
        UUID interestId, List<String> sourceIn, Timestamp publishDateFrom, Timestamp publishDateTo,
        String orderBy, String direction, String cursor, Timestamp after, int limit,
        String userId) {

        FilterDto filterDto = new FilterDto(keyword, interestId, sourceIn, publishDateFrom,
            publishDateTo);
        RequestCursorDto cursorDto = new RequestCursorDto(orderBy, direction, cursor, after, null,
            limit);

        return getArticles(filterDto, cursorDto, userId);
    }

    public PaginatedResponseDto<NewsArticleResponseDto> getRecentArticles(int limit,
        String userId) {
        // 통합 메서드 사용: 최근 뉴스 = 날짜 내림차순 정렬
        List<NewsArticle> articles = newsArticleRepository.findArticlesWithFiltersAndSorting(
            null, null, null, null, "publishDate", "DESC", null, limit);

        Map<UUID, Boolean> viewedStatusMap = Collections.emptyMap();

        List<NewsArticleResponseDto> responseDtos = articles.stream().map(article -> {
            Long actualCommentCount = commentRepository.countActiveCommentsByArticleId(
                article.getId());
            return newsArticleMapper.toDto(article,
                viewedStatusMap.getOrDefault(article.getId(), false), actualCommentCount);
        }).collect(Collectors.toList());

        return PaginatedResponseDto.<NewsArticleResponseDto>builder().content(responseDtos)
            .nextCursor(null).nextAfter(null).nextCursorViewCount(null).size(articles.size())
            .totalElements(articles.size()).hasNext(false).build();
    }

    public PaginatedResponseDto<NewsArticleResponseDto> searchArticlesByKeyword(String keyword,
        int limit, String userId) {
        // 통합 메서드 사용: 키워드 검색 + 날짜 내림차순 정렬
        List<NewsArticle> articles = newsArticleRepository.findArticlesWithFiltersAndSorting(
            keyword, null, null, null, "publishDate", "DESC", null, limit);

        logger.info("키워드 '{}' 검색 결과: {}개 기사", keyword, articles.size());

        Map<UUID, Boolean> viewedStatusMap = Collections.emptyMap();

        List<NewsArticleResponseDto> responseDtos = articles.stream().map(article -> {
            Long actualCommentCount = commentRepository.countActiveCommentsByArticleId(
                article.getId());
            return newsArticleMapper.toDto(article,
                viewedStatusMap.getOrDefault(article.getId(), false), actualCommentCount);
        }).collect(Collectors.toList());

        return PaginatedResponseDto.<NewsArticleResponseDto>builder().content(responseDtos)
            .nextCursor(null).nextAfter(null).nextCursorViewCount(null).size(articles.size())
            .totalElements(articles.size()).hasNext(false).build();
    }

    public PaginatedResponseDto<NewsArticleResponseDto> searchArticlesByKeywordAdvanced(
        String keyword, String orderBy, String direction, int limit, String userId) {

        // 기본값 설정
        if (orderBy == null || orderBy.trim().isEmpty()) {
            orderBy = "publishDate";
        }
        if (direction == null || direction.trim().isEmpty()) {
            direction = "DESC";
        }

        // 빈 문자열을 null로 변환 (쿼리에서 조건 무시하도록)
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        List<NewsArticle> articles = newsArticleRepository.findArticlesWithFiltersAndSorting(keyword,
            null, null, null, orderBy, direction, null, limit);

        logger.info("향상된 키워드 '{}' 검색 결과: {}개 기사 (정렬: {} {})", keyword, articles.size(), orderBy,
            direction);

        Map<UUID, Boolean> viewedStatusMap = Collections.emptyMap();

        List<NewsArticleResponseDto> responseDtos = articles.stream().map(article -> {
            // 실제 댓글 수 계산
            Long actualCommentCount = commentRepository.countActiveCommentsByArticleId(
                article.getId());
            return newsArticleMapper.toDto(article,
                viewedStatusMap.getOrDefault(article.getId(), false), actualCommentCount);
        }).collect(Collectors.toList());

        return PaginatedResponseDto.<NewsArticleResponseDto>builder().content(responseDtos)
            .nextCursor(null).nextAfter(null).nextCursorViewCount(null).size(articles.size())
            .totalElements(articles.size()).hasNext(false).build();
    }


}
