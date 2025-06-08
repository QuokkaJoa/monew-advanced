package com.part2.monew.service;

import com.part2.monew.config.BatchConfig;
import com.part2.monew.config.NewsProviderProperties;
import com.part2.monew.dto.request.NewsArticleDto;
import com.part2.monew.entity.Interest;
import com.part2.monew.entity.InterestNewsArticle;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.repository.InterestKeywordRepository;
import com.part2.monew.repository.InterestNewsArticleRepository;
import com.part2.monew.repository.InterestRepository;

import com.part2.monew.service.impl.NewsArticleService;
import com.part2.monew.service.impl.NewsCrawlingApiServiceImpl;
import com.part2.monew.service.newsprovider.NewsProvider;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class SpringBatch {

    private static final Logger logger = LoggerFactory.getLogger(SpringBatch.class);
    
    private final BatchConfig batchConfig;
    private final NewsCrawlingApiServiceImpl newsCrawlingService;
    private final NewsArticleService newsArticleService;
    private final InterestRepository interestRepository;
    private final InterestKeywordRepository interestKeywordRepository;
    private final InterestNewsArticleRepository interestNewsArticleRepository;
    private final NewsProviderProperties newsProviderProperties;
    private final List<NewsProvider> newsProviders;
    private final SimpleNewsCollectionService simpleNewsCollectionService;
    private final NewsBackupS3Manager newsBackupS3Manager;

    public SpringBatch(BatchConfig batchConfig, 
                      NewsCrawlingApiServiceImpl newsCrawlingService,
                      NewsArticleService newsArticleService,
                      InterestRepository interestRepository,
                      InterestKeywordRepository interestKeywordRepository,
                      InterestNewsArticleRepository interestNewsArticleRepository,
                      NewsProviderProperties newsProviderProperties,
                      List<NewsProvider> newsProviders,
                      SimpleNewsCollectionService simpleNewsCollectionService,
                      NewsBackupS3Manager newsBackupS3Manager) {
        this.batchConfig = batchConfig;
        this.newsCrawlingService = newsCrawlingService;
        this.newsArticleService = newsArticleService;
        this.interestRepository = interestRepository;
        this.interestKeywordRepository = interestKeywordRepository;
        this.interestNewsArticleRepository = interestNewsArticleRepository;
        this.newsProviderProperties = newsProviderProperties;
        this.newsProviders = newsProviders;
        this.simpleNewsCollectionService = simpleNewsCollectionService;
        this.newsBackupS3Manager = newsBackupS3Manager;
    }

    
    // 뉴스 수집 배치 활성화  
    //실험용 1분마다
    @Scheduled(cron = "0 */1 * * * *") 
    public void executeNewsCollectionBatch() {
        if (!batchConfig.isEnabled()) {
            logger.info("뉴스 수집 배치가 비활성화되어 있습니다.");
            return;
        }

        logger.info("=== 뉴스 수집 배치 시작 (매시간) ===");
        
        try {
            // 활성화된 관심사 키워드를 포함하는 뉴스만 수집
            List<NewsArticle> collectedArticles = collectNewsWithKeywordFilter();
            
            logger.info("=== 뉴스 수집 배치 완료: 총 {}개 기사 수집 ===", collectedArticles.size());
            
        } catch (Exception e) {
            logger.error("뉴스 수집 배치 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 */1 * * * *") // 1분마다 실행 (실험용)
    public void executeSimpleKeywordMatchingBatch() {
        if (!batchConfig.isEnabled()) {
            return;
        }

        logger.info("=== 하드코딩 키워드 확장 사전 뉴스 수집 배치 시작 (1분마다) ===");
        
        try {
            List<NewsArticle> collectedArticles = simpleNewsCollectionService.collectNewsWithSimpleKeywordMatching();
            logger.info("=== 하드코딩 키워드 확장 사전 뉴스 수집 배치 완료: 총 {}개 기사 저장 ===", collectedArticles.size());
            
        } catch (Exception e) {
            logger.error("간단한 키워드 매칭 배치 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    public void executeDailyNewsBackupBatch() {
        if (!batchConfig.isEnabled()) {
            logger.info("일일 뉴스 백업 배치가 비활성화되어 있습니다.");
            return;
        }

        logger.info("=== 일일 뉴스 백업 배치 시작 (자정) ===");
        
        try {
            performDailyNewsBackup();
            logger.info("=== 일일 뉴스 백업 배치 완료 ===");
            
        } catch (Exception e) {
            logger.error("일일 뉴스 백업 배치 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private List<NewsArticle> collectNewsWithKeywordFilter() {
        
        // 1. 활성화된 관심사와 연결된 키워드들만 조회
        Map<String, List<String>> activeInterestKeywordsMap = getActiveInterestKeywords();
        if (activeInterestKeywordsMap.isEmpty()) {
            logger.warn("활성화된 관심사 키워드가 없습니다.");
            return Arrays.asList();
        }
        
        logger.info("활성화된 관심사 {}개의 키워드로 뉴스 수집 시작: {}", 
                   activeInterestKeywordsMap.size(), activeInterestKeywordsMap.keySet());
        
        List<NewsArticle> allCollectedArticles = new ArrayList<>();
        
        // API 
        Map<String, List<NewsArticle>> apiArticlesByInterest = collectNewsFromApiWithKeywords(activeInterestKeywordsMap);
        apiArticlesByInterest.values().forEach(allCollectedArticles::addAll);
        
        // RSS
        Map<String, List<NewsArticle>> rssArticlesByProvider = collectNewsFromRssWithKeywordFilter(activeInterestKeywordsMap);
        rssArticlesByProvider.values().forEach(allCollectedArticles::addAll);
        
        // 중복 제거 및 DB 저장
        List<NewsArticle> savedArticles = saveUniqueArticles(allCollectedArticles);
        
        //  매핑 저장
        saveInterestMappings(savedArticles, apiArticlesByInterest, rssArticlesByProvider, activeInterestKeywordsMap);
        
        int totalApiArticles = apiArticlesByInterest.values().stream().mapToInt(List::size).sum();
        int totalRssArticles = rssArticlesByProvider.values().stream().mapToInt(List::size).sum();
        
        logger.info("=== 키워드 필터링 뉴스 수집 완료: API {}개, RSS {}개, 저장 {}개 ===", 
                   totalApiArticles, totalRssArticles, savedArticles.size());
        
        return savedArticles;
    }
    
    private Map<String, List<String>> getActiveInterestKeywords() {
        logger.info("사용자 등록 관심사 키워드 조회 시작");
        
        // 모든 관심사 조회 (실제 사용자가 등록한 것들)
        List<Interest> allInterests = interestRepository.findAll();
            
        if (allInterests.isEmpty()) {
            logger.warn("등록된 관심사가 없습니다.");
            return Map.of();
        }
        
        Map<String, List<String>> interestKeywordsMap = new HashMap<>();
        
        for (Interest interest : allInterests) {
            // 사용자가 입력한 정확한 키워드만 사용
            List<String> keywords = interestKeywordRepository.findKeywordsByInterestName(interest.getName());
                
            if (!keywords.isEmpty()) {
                interestKeywordsMap.put(interest.getName(), keywords);
                logger.info("관심사 '{}': {}개 키워드 - {}", interest.getName(), keywords.size(), keywords);
            }
        }
        
        logger.info("총 {}개 관심사의 키워드 조회 완료", interestKeywordsMap.size());
        return interestKeywordsMap;
    }
    
    // API 방식

    private Map<String, List<NewsArticle>> collectNewsFromApiWithKeywords(Map<String, List<String>> activeInterestKeywordsMap) {
        logger.info("-- API 방식 활성화된 키워드 뉴스 수집 시작 --");
        Map<String, List<NewsArticle>> apiArticlesByInterest = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : activeInterestKeywordsMap.entrySet()) {
            String interestName = entry.getKey();
            List<String> keywords = entry.getValue();
            
            try {
                logger.info("API - 활성화된 관심사 '{}': 키워드 {}개로 수집 - {}", interestName, keywords.size(), keywords);
                
                // 활성화된 키워드별로만 뉴스 수집 (네이버 API는 키워드 검색 지원)
                List<NewsArticle> articles = newsCrawlingService.collectNewsForKeywords(keywords, 10);
                apiArticlesByInterest.put(interestName, articles);
                
                logger.info("API - 활성화된 관심사 '{}': {}개 기사 수집 완료", interestName, articles.size());
                
            } catch (Exception e) {
                logger.error("API - 활성화된 관심사 '{}' 수집 중 오류: {}", interestName, e.getMessage());
            }
        }
        
        logger.info("-- API 방식 활성화된 키워드 뉴스 수집 완료: 총 {}개 기사 --", apiArticlesByInterest.values().stream().mapToInt(List::size).sum());
        return apiArticlesByInterest;
    }
    // RSS 방식
    private Map<String, List<NewsArticle>> collectNewsFromRssWithKeywordFilter(Map<String, List<String>> activeInterestKeywordsMap) {
        logger.info("-- RSS 방식 뉴스 수집 시작 --");
        Map<String, List<NewsArticle>> rssArticlesByProvider = new HashMap<>();
        
        // 모든 키워드 수집 (RSS 필터링용)
        List<String> allKeywords = activeInterestKeywordsMap.values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
        
        logger.info("RSS 수집용 키워드 {}개: {}", allKeywords.size(), allKeywords);
        
        // 활성화된 RSS 프로바이더들 필터링
        Map<String, NewsProviderProperties.ProviderConfig> enabledRssProviders = newsProviderProperties.getProviders()
                .entrySet().stream()
                .filter(entry -> "rss".equals(entry.getValue().getType()) && entry.getValue().isEnabled())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                
        logger.info("활성화된 RSS 프로바이더 {}개: {}", enabledRssProviders.size(), enabledRssProviders.keySet());
        
        if (enabledRssProviders.isEmpty()) {
            logger.warn("활성화된 RSS 프로바이더가 없습니다.");
            return rssArticlesByProvider;
        }
        
        // 사용 가능한 NewsProvider들 확인
        logger.info("등록된 NewsProvider {}개:", newsProviders.size());
        for (NewsProvider provider : newsProviders) {
            logger.info("  - {}", provider.getProviderKey());
        }
        
        for (Map.Entry<String, NewsProviderProperties.ProviderConfig> entry : enabledRssProviders.entrySet()) {
            String providerKey = entry.getKey();
            NewsProviderProperties.ProviderConfig config = entry.getValue();
            
            logger.info("RSS 프로바이더 '{}' 처리 시작 - 이름: {}, URL: {}", providerKey, config.getName(), config.getFeedUrl());
            
            try {
                // 해당 프로바이더에 맞는 NewsProvider 찾기
                NewsProvider provider = newsProviders.stream()
                        .filter(p -> p.getProviderKey().contains("RSS") || p.getProviderKey().contains("Feed"))
                        .findFirst()
                        .orElse(null);
                
                if (provider == null) {
                    logger.warn("RSS 프로바이더 '{}'에 대한 NewsProvider를 찾을 수 없습니다", providerKey);
                    continue;
                }
                
                logger.info("NewsProvider '{}' 찾음, RSS 수집 시작", provider.getProviderKey());
                
                // 사용자가 등록한 키워드로 필터링해서 수집
                List<NewsArticleDto> keywordFilteredDtos = provider.fetchNews(config, allKeywords);
                
                logger.info("RSS 프로바이더 '{}': {}개 기사 수집됨", providerKey, keywordFilteredDtos.size());
                
                List<NewsArticle> articles = convertDtosToEntities(keywordFilteredDtos);
                rssArticlesByProvider.put(providerKey, articles);
                
                logger.info("RSS 프로바이더 '{}': {}개 기사 엔티티로 변환 완료", providerKey, articles.size());
                
            } catch (Exception e) {
                logger.error("RSS - '{}' 수집 중 오류: {}", config.getName(), e.getMessage(), e);
            }
        }
        
        logger.info("-- RSS 방식 활성화된 키워드 뉴스 수집 완료: 총 {}개 기사 --", rssArticlesByProvider.values().stream().mapToInt(List::size).sum());
        return rssArticlesByProvider;
    }
    

    private List<NewsArticle> convertDtosToEntities(List<NewsArticleDto> dtos) {
        return dtos.stream()
                .map(dto -> NewsArticle.builder()
                        .sourceIn(dto.getProviderName())
                        .sourceUrl(dto.getOriginalLink())
                        .title(dto.getTitle())
                        .publishedDate(dto.getPublishedDate())
                        .summary(dto.getSummaryOrContent())
                        .viewCount(0L)
                        .build())
                .collect(Collectors.toList());
    }
    
    // 중복 체크    
    private List<NewsArticle> saveUniqueArticles(List<NewsArticle> articles) {
        
        if (articles.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 가져 올 떄 중복 제거
        List<NewsArticle> uniqueArticles = articles.stream()
                .filter(article -> article.getSourceUrl() != null && !article.getSourceUrl().trim().isEmpty())
                .collect(Collectors.toMap(
                        NewsArticle::getSourceUrl,
                        article -> article,
                        (existing, replacement) -> existing // 중복 시 기존 것 유지
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
                
        

        // DB에 
        List<String> sourceUrls = uniqueArticles.stream()
                .map(NewsArticle::getSourceUrl)
                .collect(Collectors.toList());
                
        List<String> existingUrls = newsArticleService.getNewsArticleRepository().findExistingSourceUrls(sourceUrls);
        
        List<NewsArticle> newArticles = uniqueArticles.stream()
                .filter(article -> !existingUrls.contains(article.getSourceUrl()))
                .collect(Collectors.toList());
                
        logger.info("DB 중복 제거 완료: {}개 → {}개 (새로운 기사)", uniqueArticles.size(), newArticles.size());
        
        // DB 저장
        if (!newArticles.isEmpty()) {
            List<NewsArticle> savedArticles = newsArticleService.getNewsArticleRepository().saveAll(newArticles);
            logger.info("DB 저장 완료: {}개 기사", savedArticles.size());
            return savedArticles;
        }
        
        logger.info("저장할 새로운 기사가 없습니다.");
        return new ArrayList<>();
    }

    private void saveInterestMappings(List<NewsArticle> savedArticles, Map<String, List<NewsArticle>> apiArticlesByInterest, Map<String, List<NewsArticle>> rssArticlesByProvider, Map<String, List<String>> activeInterestKeywordsMap) {
        logger.info("뉴스-관심사 매핑 저장 시작: {}개 기사", savedArticles.size());
        
        // 관심사 조회
        Map<String, Interest> interestMap = interestRepository.findAll().stream()
                .collect(Collectors.toMap(Interest::getName, interest -> interest));
        
        int totalMappings = 0;
        
        // API에서 수집된 기사들의 매핑 저장
        for (Map.Entry<String, List<NewsArticle>> entry : apiArticlesByInterest.entrySet()) {
            String interestName = entry.getKey();
            List<NewsArticle> articles = entry.getValue();
            
            Interest interest = interestMap.get(interestName);
            if (interest != null) {
                for (NewsArticle article : articles) {
                    // 저장된 기사 중에서 해당 기사 찾기 (sourceUrl로 매칭)
                    NewsArticle savedArticle = savedArticles.stream()
                            .filter(saved -> saved.getSourceUrl().equals(article.getSourceUrl()))
                            .findFirst()
                            .orElse(null);
                    
                    if (savedArticle != null) {
                        // 중복 매핑 방지
                        if (!interestNewsArticleRepository.existsByNewsArticleIdAndInterestId(savedArticle.getId(), interest.getId())) {
                            InterestNewsArticle mapping = InterestNewsArticle.create(interest, savedArticle);
                            interestNewsArticleRepository.save(mapping);
                            totalMappings++;
                            logger.debug("API 매핑 생성: 기사 '{}' → 관심사 '{}'", savedArticle.getTitle(), interestName);
                        }
                    }
                }
            }
        }
        
        // RSS에서 수집된 기사들의 매핑 저장 (모든 관심사의 키워드로 필터링된 것들)
        for (Map.Entry<String, List<NewsArticle>> entry : rssArticlesByProvider.entrySet()) {
            String providerKey = entry.getKey();
            List<NewsArticle> articles = entry.getValue();
            
            for (NewsArticle article : articles) {
                // 저장된 기사 중에서 해당 기사 찾기
                NewsArticle savedArticle = savedArticles.stream()
                        .filter(saved -> saved.getSourceUrl().equals(article.getSourceUrl()))
                        .findFirst()
                        .orElse(null);
                
                if (savedArticle != null) {
                    // RSS 기사는 모든 관심사에 대해 키워드 필터링을 통과한 것이므로,
                    // 각 관심사별로 다시 매칭 확인
                    for (String interestName : activeInterestKeywordsMap.keySet()) {
                        Interest interest = interestMap.get(interestName);
                        if (interest != null) {
                            // CategoryKeywordService를 사용하여 실제로 이 기사가 해당 관심사와 매칭되는지 확인
                            if (isArticleMatchedToInterest(savedArticle, interestName, activeInterestKeywordsMap.get(interestName))) {
                                // 중복 매핑 방지
                                if (!interestNewsArticleRepository.existsByNewsArticleIdAndInterestId(savedArticle.getId(), interest.getId())) {
                                    InterestNewsArticle mapping = InterestNewsArticle.create(interest, savedArticle);
                                    interestNewsArticleRepository.save(mapping);
                                    totalMappings++;
                                    logger.debug("RSS 매핑 생성: 기사 '{}' → 관심사 '{}'", savedArticle.getTitle(), interestName);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        logger.info("뉴스-관심사 매핑 저장 완료: {}개 매핑 생성", totalMappings);
    }
    

    private boolean isArticleMatchedToInterest(NewsArticle article, String interestName, List<String> keywords) {
        String title = article.getTitle() != null ? article.getTitle() : "";
        String content = article.getSummary() != null ? article.getSummary() : "";
        String searchText = (title + " " + content).toLowerCase();
        
        // 키워드 중 하나라도 매칭되면 true
        return keywords.stream().anyMatch(keyword -> searchText.contains(keyword.toLowerCase()));
    }


    private void performDailyNewsBackup() {
        try {
            logger.info("=== 일일 뉴스 백업 시작 ===");

            // 전체 활성 뉴스 기사 조회 (삭제되지 않은 것들)
            List<NewsArticle> allActiveArticles = newsArticleService.getNewsArticleRepository().findAll().stream()
                .filter(article -> !article.isDeleted())
                .collect(Collectors.toList());
            
            if (allActiveArticles.isEmpty()) {
                logger.info("백업할 뉴스 기사가 없습니다.");
                return;
            }

            LocalDate today = LocalDate.now();
            String backupKey = "daily-news-backup-" + today + "-" + System.currentTimeMillis() + ".json";

            StringBuilder json = new StringBuilder();
            json.append("{\"backup_date\":\"").append(today).append("\",");
            json.append("\"backup_type\":\"daily\",");
            json.append("\"total_count\":").append(allActiveArticles.size()).append(",");
            json.append("\"articles\":[");

            for (int i = 0; i < allActiveArticles.size(); i++) {
                NewsArticle article = allActiveArticles.get(i);
                json.append("{")
                    .append("\"id\":\"").append(article.getId()).append("\",")
                    .append("\"title\":\"").append(escapeJson(article.getTitle())).append("\",")
                    .append("\"source\":\"").append(escapeJson(article.getSourceIn())).append("\",")
                    .append("\"url\":\"").append(escapeJson(article.getSourceUrl())).append("\",")
                    .append("\"published_date\":\"").append(article.getPublishedDate()).append("\",")
                    .append("\"view_count\":").append(article.getViewCount() != null ? article.getViewCount() : 0).append(",")
                    .append("\"comment_count\":").append(article.getCommentCount() != null ? article.getCommentCount() : 0)
                    .append("}");
                if (i < allActiveArticles.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]}");

            // S3 업로드
            newsBackupS3Manager.uploadNewsBackup(json.toString().getBytes(StandardCharsets.UTF_8), backupKey);

            logger.info("=== 일일 뉴스 백업 완료: {}개 기사 백업, 파일: {} ===", allActiveArticles.size(), backupKey);

        } catch (Exception e) {
            logger.error("일일 뉴스 백업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * JSON 문자열 이스케이프 처리
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}

