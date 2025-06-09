package com.part2.monew.service;

import com.part2.monew.config.NewsProviderProperties;
import com.part2.monew.dto.request.NewsArticleDto;
import com.part2.monew.entity.Interest;
import com.part2.monew.entity.InterestNewsArticle;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.entity.UserSubscriber;
import com.part2.monew.repository.InterestKeywordRepository;
import com.part2.monew.repository.InterestNewsArticleRepository;
import com.part2.monew.repository.InterestRepository;
import com.part2.monew.repository.UserSubscriberRepository;
import com.part2.monew.service.impl.NewsArticleService;
import com.part2.monew.service.impl.NewsCrawlingApiServiceImpl;
import com.part2.monew.service.newsprovider.NewsProvider;
import com.part2.monew.service.CategoryKeywordService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SimpleNewsCollectionService {

    private final NewsCrawlingApiServiceImpl newsCrawlingService;
    private final NewsArticleService newsArticleService;
    private final InterestRepository interestRepository;
    private final InterestKeywordRepository interestKeywordRepository;
    private final InterestNewsArticleRepository interestNewsArticleRepository;
    private final UserSubscriberRepository userSubscriberRepository;
    private final NewsProviderProperties newsProviderProperties;
    private final List<NewsProvider> newsProviders;
    private final NewsBackupS3Manager newsBackupS3Manager;
    private final CategoryKeywordService categoryKeywordService;

    public SimpleNewsCollectionService(NewsCrawlingApiServiceImpl newsCrawlingService,
        NewsArticleService newsArticleService, InterestRepository interestRepository,
        InterestKeywordRepository interestKeywordRepository,
        InterestNewsArticleRepository interestNewsArticleRepository,
        UserSubscriberRepository userSubscriberRepository,
        NewsProviderProperties newsProviderProperties, List<NewsProvider> newsProviders,
        NewsBackupS3Manager newsBackupS3Manager, CategoryKeywordService categoryKeywordService) {
        this.newsCrawlingService = newsCrawlingService;
        this.newsArticleService = newsArticleService;
        this.interestRepository = interestRepository;
        this.interestKeywordRepository = interestKeywordRepository;
        this.interestNewsArticleRepository = interestNewsArticleRepository;
        this.userSubscriberRepository = userSubscriberRepository;
        this.newsProviderProperties = newsProviderProperties;
        this.newsProviders = newsProviders;
        this.newsBackupS3Manager = newsBackupS3Manager;
        this.categoryKeywordService = categoryKeywordService;
        
        log.info("SimpleNewsCollectionService 초기화 완료 - CategoryKeywordService 사용");
    }

    public List<NewsArticle> collectNewsWithSimpleKeywordMatching() {
        log.info("=== 간단한 키워드 매칭 뉴스 수집 시작 ===");

        Map<String, List<String>> interestKeywordsMap = getUserInterestKeywords();
        if (interestKeywordsMap.isEmpty()) {
            log.warn("활성화된 관심사가 없습니다. 수집을 건너뜁니다.");
            return new ArrayList<>();
        }

        log.info("관심사 {}개: {}", interestKeywordsMap.size(), interestKeywordsMap.keySet());

        Set<String> allKeywords = interestKeywordsMap.values().stream().flatMap(List::stream)
            .collect(Collectors.toSet());

        log.info("실시간 매칭할 키워드 {}개: {}", allKeywords.size(), allKeywords);

        List<NewsArticle> matchedArticles = new ArrayList<>();

        // API 방식 수집 (2단계 필터링)
        matchedArticles.addAll(collectFromApiWithSmartFilter(allKeywords));

        // RSS 방식 수집 활성화
        log.info("RSS 수집 시작");
        matchedArticles.addAll(collectFromRssWithSmartFilter(allKeywords));

        log.info("실시간 키워드 매칭으로 수집된 기사: {}개", matchedArticles.size());

        // 중복 제거
        List<NewsArticle> savedArticles = saveUniqueArticles(matchedArticles);
        log.info("DB 저장 완료: {}개", savedArticles.size());

        // 관심사 매핑 저장
        saveInterestMappings(savedArticles, interestKeywordsMap);

        log.info("=== 간단한 키워드 매칭 뉴스 수집 완료: {}개 기사 저장 ===", savedArticles.size());
        return savedArticles;
    }

    private Map<String, List<String>> getUserInterestKeywords() {
        // 실제 사용자들이 구독한 관심사만 가져오기
        List<UserSubscriber> subscribedInterests = userSubscriberRepository.findAll();
        Map<String, List<String>> interestKeywordsMap = new HashMap<>();
        Set<String> processedInterests = new HashSet<>();

        for (UserSubscriber subscription : subscribedInterests) {
            Interest interest = subscription.getInterest();
            String interestName = interest.getName();
            
            // 중복 처리 방지
            if (processedInterests.contains(interestName)) {
                continue;
            }
            processedInterests.add(interestName);

            List<String> originalKeywords = interestKeywordRepository.findKeywordsByInterestName(interestName);
            if (!originalKeywords.isEmpty()) {
                // CategoryKeywordService로 키워드 확장
                Set<String> expandedKeywords = expandKeywordsWithCategoryService(originalKeywords);
                interestKeywordsMap.put(interestName, new ArrayList<>(expandedKeywords));

                log.info("구독된 관심사 '{}': {}개 → {}개 키워드 확장", interestName, originalKeywords.size(),
                    expandedKeywords.size());
                log.info("  원본: {}", originalKeywords);
                log.info("  확장: {}", expandedKeywords);
            }
        }

        if (interestKeywordsMap.isEmpty()) {
            log.warn("구독된 관심사가 없습니다. 아무도 관심사를 구독하지 않았습니다.");
        } else {
            log.info("총 {}개의 구독된 관심사에서 키워드 확장", interestKeywordsMap.size());
        }

        return interestKeywordsMap;
    }

    /**
     * CategoryKeywordService를 사용하여 키워드 확장
     */
    private Set<String> expandKeywordsWithCategoryService(List<String> originalKeywords) {
        Set<String> expandedKeywords = new HashSet<>(originalKeywords); // 원본 키워드 포함

        for (String keyword : originalKeywords) {
            // CategoryKeywordService에서 해당 키워드가 속한 카테고리 찾기
            for (String category : categoryKeywordService.getAllCategories()) {
                if (categoryKeywordService.isKeywordInCategory(keyword, category)) {
                    List<String> categoryKeywords = categoryKeywordService.getKeywordsForCategory(category);
                    expandedKeywords.addAll(categoryKeywords);
                    log.debug("키워드 '{}' 카테고리 '{}' 확장: {}개 키워드 추가", keyword, category, categoryKeywords.size());
                    break; // 첫 번째 매칭 카테고리만 사용
                }
            }
        }

        return expandedKeywords;
    }

    /**
     * API 방식으로 뉴스 수집 + 스마트 키워드 기반 수집 전략 - 하드코딩 사전에 있는 키워드: 특화 피드에서 20개 수집 - 하드코딩 사전에 없는 키워드: 전체
     * 기사에서 50개 수집 후 필터링
     */
    private List<NewsArticle> collectFromApiWithSmartFilter(Set<String> keywords) {
        log.info("-- API 방식 뉴스 수집 + 스마트 키워드 전략 --");

        try {
            // CategoryKeywordService에 등록된 키워드인지 확인
            List<String> categoryMatchedKeywords = new ArrayList<>();
            List<String> unknownKeywords = new ArrayList<>();

            for (String keyword : keywords) {
                boolean foundInCategory = false;
                for (String category : categoryKeywordService.getAllCategories()) {
                    if (categoryKeywordService.isKeywordInCategory(keyword, category)) {
                        categoryMatchedKeywords.add(keyword);
                        log.info("카테고리 매칭 키워드: '{}' (카테고리: {})", keyword, category);
                        foundInCategory = true;
                        break;
                    }
                }
                if (!foundInCategory) {
                    unknownKeywords.add(keyword);
                    log.info("카테고리에 없는 키워드: '{}'", keyword);
                }
            }

            List<NewsArticle> allArticles = new ArrayList<>();

            if (!categoryMatchedKeywords.isEmpty()) {
                log.info("=== 특화 피드 수집: {}개 키워드로 20개 기사 수집 ===", categoryMatchedKeywords.size());
                List<NewsArticle> specializedArticles = newsCrawlingService.collectNewsForKeywords(
                    categoryMatchedKeywords, 20);
                allArticles.addAll(specializedArticles);
                log.info("특화 피드에서 {}개 기사 수집됨", specializedArticles.size());
            }

            if (!unknownKeywords.isEmpty() || allArticles.size() < 10) {
                log.info("=== 전체 피드 수집: 50개 기사에서 키워드 매칭 ===");
                List<String> generalKeywords = Arrays.asList("한국", "뉴스", "사회", "경제", "정치");
                List<NewsArticle> generalArticles = newsCrawlingService.collectNewsForKeywords(
                    generalKeywords, 50);
                allArticles.addAll(generalArticles);
                log.info("전체 피드에서 {}개 기사 수집됨", generalArticles.size());
            }

            // 중복 제거
            allArticles = new ArrayList<>(allArticles.stream().collect(Collectors.toMap(
                    article -> article.getSourceUrl() != null ? article.getSourceUrl()
                        : article.getTitle(), article -> article, (existing, replacement) -> existing))
                .values());

            log.info("중복 제거 후 총 {}개 기사", allArticles.size());

            List<NewsArticle> matchedArticles = new ArrayList<>();
            List<NewsArticle> needContentCheck = new ArrayList<>();

            // 1단계: 제목+요약에서 빠른 매칭
            for (NewsArticle article : allArticles) {
                if (containsKeywordInTitleOrSummary(article, keywords)) {
                    matchedArticles.add(article);
                    log.debug("1단계 매칭 (제목/요약): {}",
                        article.getTitle().length() > 50 ? article.getTitle().substring(0, 50)
                            + "..." : article.getTitle());
                } else {
                    needContentCheck.add(article);
                }
            }

            // 2단계: 본문에서 키워드 확인 (1단계에서 놓친 기사들)
            for (NewsArticle article : needContentCheck) {
                if (containsKeywordInContent(article, keywords)) {
                    matchedArticles.add(article);
                    log.debug("2단계 매칭 (본문): {}",
                        article.getTitle().length() > 50 ? article.getTitle().substring(0, 50)
                            + "..." : article.getTitle());
                }
            }

            log.info("API 최종 결과: {}개 수집 → {}개 매칭 (1단계: {}개, 2단계: {}개)", allArticles.size(),
                matchedArticles.size(),
                matchedArticles.size() - (matchedArticles.size() - (allArticles.size()
                    - needContentCheck.size())),
                matchedArticles.size() - (allArticles.size() - needContentCheck.size()));

            return matchedArticles;

        } catch (Exception e) {
            log.error("API 뉴스 수집 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsArticle> collectFromRssWithSmartFilter(Set<String> keywords) {
        log.info("-- RSS 방식 뉴스 수집 (빠른 처리) --");

        List<NewsArticle> allRssArticles = new ArrayList<>();

        // 활성화된 RSS 프로바이더만 필터링 (제한 해제 - 모든 RSS 소스 사용)
        Map<String, NewsProviderProperties.ProviderConfig> enabledRssProviders = newsProviderProperties.getProviders()
            .entrySet().stream().filter(
                entry -> "rss".equals(entry.getValue().getType()) && entry.getValue().isEnabled())
            .limit(15) // 최대 15개 RSS 소스로 확대 (34개 중 성능 고려)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("활성화된 RSS 프로바이더: {}개 (다양한 언론사)", enabledRssProviders.size());

        // RSS Provider 찾기
        NewsProvider rssProvider = newsProviders.stream()
            .filter(p -> p.getProviderKey().contains("RSS") || p.getProviderKey().contains("Feed"))
            .findFirst().orElse(null);

        if (rssProvider == null) {
            log.warn("RSS NewsProvider를 찾을 수 없습니다.");
            return allRssArticles;
        }

        // 각 RSS 소스별로 빠른 수집 (시간 제한)
        for (Map.Entry<String, NewsProviderProperties.ProviderConfig> entry : enabledRssProviders.entrySet()) {
            String providerKey = entry.getKey();
            NewsProviderProperties.ProviderConfig config = entry.getValue();

            try {
                log.info("RSS '{}' 수집 중... (타임아웃 10초)", config.getName());

                long startTime = System.currentTimeMillis();

                // 키워드 없이 기사 수집 (빠른 처리)
                List<NewsArticleDto> dtos = rssProvider.fetchNews(config, new ArrayList<>());
                List<NewsArticle> articles = convertDtosToEntities(dtos);

                // 각 RSS당 최대 10개로 확대
                if (articles.size() > 10) {
                    articles = articles.subList(0, 10);
                }

                allRssArticles.addAll(articles);

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("RSS '{}': {}개 기사 수집 ({}ms)", config.getName(), articles.size(), elapsed);

                // 개별 RSS 처리가 10초 이상 걸리면 중단
                if (elapsed > 10000) {
                    log.warn("RSS '{}' 처리 시간 초과 ({}ms), 다음 소스로 이동", config.getName(), elapsed);
                }

            } catch (Exception e) {
                log.error("RSS '{}' 수집 실패 (빠른 처리): {}", config.getName(), e.getMessage());
            }
        }

        log.info("RSS 총 {}개 기사 수집 완료, 키워드 매칭 시작", allRssArticles.size());

        // RSS 기사들도 키워드 매칭 필터링 적용
        List<NewsArticle> matchedRssArticles = new ArrayList<>();
        for (NewsArticle article : allRssArticles) {
            if (containsKeywordInTitleOrSummary(article, keywords)) {
                matchedRssArticles.add(article);
                log.debug("RSS 키워드 매칭: {}",
                    article.getTitle().length() > 50 ? article.getTitle().substring(0, 50) + "..."
                        : article.getTitle());
            }
        }

        log.info("RSS 키워드 매칭 결과: {}개 → {}개", allRssArticles.size(), matchedRssArticles.size());
        return matchedRssArticles;
    }

    private List<NewsArticle> filterByKeywordMatching(List<NewsArticle> articles,
        Map<String, List<String>> interestKeywordsMap) {
        log.info("키워드 매칭 필터링 시작: {}개 기사", articles.size());

        Set<String> allKeywords = interestKeywordsMap.values().stream().flatMap(List::stream)
            .collect(Collectors.toSet());

        log.info("매칭할 키워드 {}개: {}", allKeywords.size(), allKeywords);

        List<NewsArticle> matchedArticles = new ArrayList<>();

        for (NewsArticle article : articles) {
            if (containsAnyKeyword(article, allKeywords)) {
                matchedArticles.add(article);
            }
        }

        log.info("키워드 매칭 결과: {}개 → {}개", articles.size(), matchedArticles.size());
        return matchedArticles;
    }

    private boolean containsKeywordInTitleOrSummary(NewsArticle article, Set<String> keywords) {
        String title = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
        String summary = article.getSummary() != null ? article.getSummary().toLowerCase() : "";
        String searchText = title + " " + summary;

        for (String keyword : keywords) {
            if (searchText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsKeywordInContent(NewsArticle article, Set<String> keywords) {
        // 현재는 summary를 본문으로 사용
        String content = article.getSummary() != null ? article.getSummary().toLowerCase() : "";

        for (String keyword : keywords) {
            if (content.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyKeyword(NewsArticle article, Set<String> keywords) {
        String title = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
        String content = article.getSummary() != null ? article.getSummary().toLowerCase() : "";
        String searchText = title + " " + content;

        for (String keyword : keywords) {
            if (searchText.contains(keyword.toLowerCase())) {
                log.debug("키워드 '{}' 매칭: {}", keyword,
                    title.length() > 50 ? title.substring(0, 50) + "..." : title);
                return true;
            }
        }

        return false;
    }

    private List<NewsArticle> convertDtosToEntities(List<NewsArticleDto> dtos) {
        return dtos.stream().map(dto -> NewsArticle.builder().sourceIn(dto.getProviderName())
            .sourceUrl(dto.getOriginalLink()).title(dto.getTitle())
            .publishedDate(dto.getPublishedDate())
            .summary(dto.getSummaryOrContent()).viewCount(0L).build()).collect(Collectors.toList());
    }

    private List<NewsArticle> saveUniqueArticles(List<NewsArticle> articles) {
        if (articles.isEmpty()) {
            return new ArrayList<>();
        }

        List<NewsArticle> uniqueArticles = articles.stream().filter(
                article -> article.getSourceUrl() != null && !article.getSourceUrl().trim().isEmpty())
            .collect(Collectors.toMap(NewsArticle::getSourceUrl, article -> article,
                (existing, replacement) -> existing)).values().stream()
            .collect(Collectors.toList());

        // DB에 이미 있는 URL 제외
        List<String> sourceUrls = uniqueArticles.stream().map(NewsArticle::getSourceUrl)
            .collect(Collectors.toList());

        List<String> existingUrls = newsArticleService.getNewsArticleRepository()
            .findExistingSourceUrls(sourceUrls);

        List<NewsArticle> newArticles = uniqueArticles.stream()
            .filter(article -> !existingUrls.contains(article.getSourceUrl()))
            .collect(Collectors.toList());

        log.info("중복 제거: {}개 → {}개 (새로운 기사)", uniqueArticles.size(), newArticles.size());

        if (!newArticles.isEmpty()) {
            List<NewsArticle> savedArticles = newsArticleService.getNewsArticleRepository()
                .saveAll(newArticles);
            log.info("DB 저장 완료: {}개", savedArticles.size());
            return savedArticles;
        }

        return new ArrayList<>();
    }

    private void saveInterestMappings(List<NewsArticle> savedArticles,
        Map<String, List<String>> interestKeywordsMap) {
        log.info("관심사 매핑 시작: {}개 기사", savedArticles.size());

        Map<String, Interest> interestMap = interestRepository.findAll().stream()
            .collect(Collectors.toMap(Interest::getName, interest -> interest));

        int totalMappings = 0;

        for (NewsArticle article : savedArticles) {
            for (Map.Entry<String, List<String>> entry : interestKeywordsMap.entrySet()) {
                String interestName = entry.getKey();
                List<String> keywords = entry.getValue();

                Interest interest = interestMap.get(interestName);
                if (interest != null && containsAnyKeyword(article, new HashSet<>(keywords))) {
                    // 중복 매핑 방지
                    if (!interestNewsArticleRepository.existsByNewsArticleIdAndInterestId(
                        article.getId(), interest.getId())) {
                        InterestNewsArticle mapping = InterestNewsArticle.create(interest, article);
                        interestNewsArticleRepository.save(mapping);
                        totalMappings++;
                        log.debug("매핑 생성: '{}' → '{}'",
                            article.getTitle().length() > 30 ? article.getTitle().substring(0, 30)
                                + "..." : article.getTitle(), interestName);
                    }
                }
            }
        }

        log.info("관심사 매핑 완료: {}개", totalMappings);
    }

    private void performS3Backup(List<NewsArticle> savedArticles) {
        try {
            log.info("=== S3 백업 시작: {}개 기사를 하나의 파일로 일괄 백업 ===", savedArticles.size());

            LocalDate today = LocalDate.now();
            String backupKey = "news-backup-" + today + "-" + System.currentTimeMillis() + ".json";

            StringBuilder json = new StringBuilder();
            json.append("{\"backup_date\":\"").append(today).append("\",");
            json.append("\"count\":").append(savedArticles.size()).append(",");
            json.append("\"articles\":[");

            for (int i = 0; i < savedArticles.size(); i++) {
                NewsArticle article = savedArticles.get(i);
                json.append("{").append("\"id\":").append(article.getId()).append(",")
                    .append("\"title\":\"").append(escapeJson(article.getTitle())).append("\",")
                    .append("\"source\":\"").append(escapeJson(article.getSourceIn())).append("\",")
                    .append("\"url\":\"").append(escapeJson(article.getSourceUrl())).append("\"")
                    .append("}");
                if (i < savedArticles.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]}");

            // S3 업로드
            newsBackupS3Manager.uploadNewsBackup(json.toString().getBytes(StandardCharsets.UTF_8), backupKey);

            log.info("=== S3 백업 완료: {} ===", backupKey);

        } catch (Exception e) {
            log.error("S3 백업 실패: {}", e.getMessage());
        }
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
