package com.part2.monew.service;

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
    private final NewsProviderProperties newsProviderProperties;
    private final List<NewsProvider> newsProviders;
    private final NewsBackupS3Manager newsBackupS3Manager;

    // 하드코딩 사전
    private final Map<String, List<String>> keywordExpansionMap = new HashMap<>();

    public SimpleNewsCollectionService(NewsCrawlingApiServiceImpl newsCrawlingService,
        NewsArticleService newsArticleService, InterestRepository interestRepository,
        InterestKeywordRepository interestKeywordRepository,
        InterestNewsArticleRepository interestNewsArticleRepository,
        NewsProviderProperties newsProviderProperties, List<NewsProvider> newsProviders,
        NewsBackupS3Manager newsBackupS3Manager) {
        this.newsCrawlingService = newsCrawlingService;
        this.newsArticleService = newsArticleService;
        this.interestRepository = interestRepository;
        this.interestKeywordRepository = interestKeywordRepository;
        this.interestNewsArticleRepository = interestNewsArticleRepository;
        this.newsProviderProperties = newsProviderProperties;
        this.newsProviders = newsProviders;
        this.newsBackupS3Manager = newsBackupS3Manager;
        initializeKeywordExpansionMap();
    }

    private void initializeKeywordExpansionMap() {
        keywordExpansionMap.put("뉴스",
            Arrays.asList("뉴스", "소식", "보도", "기사", "발표", "공개", "확인", "보고"));
        keywordExpansionMap.put("한국", Arrays.asList("한국", "국내", "우리나라", "대한민국", "서울", "부산", "지역"));
        keywordExpansionMap.put("정부", Arrays.asList("정부", "당국", "관계자", "담당자", "공무원", "행정부"));
        keywordExpansionMap.put("국민", Arrays.asList("국민", "시민", "주민", "사람들", "개인", "소비자"));

        // 경제 분야
        keywordExpansionMap.put("경제",
            Arrays.asList("경제", "금융", "투자", "기업", "산업", "무역", "주식", "은행", "자본", "시장", "돈", "자금",
                "재정", "예산", "물가", "가격"));
        keywordExpansionMap.put("투자",
            Arrays.asList("투자", "자본", "펀드", "주식", "채권", "증권", "자산", "수익", "이익", "손실"));
        keywordExpansionMap.put("기업",
            Arrays.asList("기업", "회사", "법인", "사업", "경영", "매출", "수익", "직장", "근무", "일자리", "취업"));
        keywordExpansionMap.put("은행",
            Arrays.asList("은행", "금융", "대출", "적금", "예금", "신용", "카드", "계좌"));

        keywordExpansionMap.put("IT",
            Arrays.asList("IT", "기술", "컴퓨터", "소프트웨어", "디지털", "인터넷", "데이터", "온라인", "웹", "앱", "스마트폰",
                "휴대폰"));
        keywordExpansionMap.put("AI",
            Arrays.asList("AI", "인공지능", "머신러닝", "딥러닝", "로봇", "자동화", "알고리즘", "빅데이터"));
        keywordExpansionMap.put("인공지능",
            Arrays.asList("인공지능", "AI", "머신러닝", "딥러닝", "신경망", "알고리즘", "자동화", "로봇"));
        keywordExpansionMap.put("컴퓨터",
            Arrays.asList("컴퓨터", "PC", "노트북", "하드웨어", "프로세서", "메모리", "게임", "인터넷"));
        keywordExpansionMap.put("소프트웨어",
            Arrays.asList("소프트웨어", "프로그램", "앱", "애플리케이션", "시스템", "개발", "코딩"));
        keywordExpansionMap.put("프로그래밍",
            Arrays.asList("프로그래밍", "개발", "코딩", "프로그램", "소프트웨어", "개발자", "엔지니어"));
        keywordExpansionMap.put("스마트폰",
            Arrays.asList("스마트폰", "휴대폰", "핸드폰", "모바일", "앱", "어플", "삼성", "애플", "아이폰"));

        // === 정치 분야 (대폭 확장) ===
        keywordExpansionMap.put("정치",
            Arrays.asList("정치", "정부", "국회", "대통령", "정책", "선거", "정당", "의원", "장관", "국정"));
        keywordExpansionMap.put("대통령", Arrays.asList("대통령", "정부", "청와대", "국정", "정책", "발표", "지시"));
        keywordExpansionMap.put("국회", Arrays.asList("국회", "의원", "법안", "정치", "여당", "야당", "국정감사"));
        keywordExpansionMap.put("선거", Arrays.asList("선거", "투표", "후보", "정당", "정치", "당선", "공약"));

        // === 사회 분야 (대폭 확장) ===
        keywordExpansionMap.put("사회",
            Arrays.asList("사회", "교육", "문화", "환경", "의료", "복지", "시민", "생활", "일상", "문제", "이슈"));
        keywordExpansionMap.put("교육",
            Arrays.asList("교육", "학교", "학생", "선생님", "교사", "대학", "입시", "공부", "학습"));
        keywordExpansionMap.put("의료",
            Arrays.asList("의료", "병원", "의사", "간호사", "치료", "수술", "약", "건강", "질병"));
        keywordExpansionMap.put("환경",
            Arrays.asList("환경", "기후", "오염", "에너지", "재활용", "자연", "공기", "물"));
        keywordExpansionMap.put("문화", Arrays.asList("문화", "예술", "전시", "책", "축제", "미술", "조각"));
        keywordExpansionMap.put("연예",
            Arrays.asList("연예", "연예인", "가수", "배우", "드라마", "영화", "음악", "공연", "콘서트", "방송"));

        // === 스포츠 분야 (대폭 확장) ===
        keywordExpansionMap.put("스포츠",
            Arrays.asList("스포츠", "축구", "야구", "올림픽", "경기", "선수", "팀", "운동", "체육"));
        keywordExpansionMap.put("축구", Arrays.asList("축구", "월드컵", "선수", "골", "경기", "팀", "리그"));
        keywordExpansionMap.put("야구", Arrays.asList("야구", "선수", "홈런", "경기", "팀", "리그", "승부"));
        keywordExpansionMap.put("올림픽", Arrays.asList("올림픽", "선수", "메달", "경기", "국가대표", "스포츠"));

        // === 일상생활 키워드 추가 ===
        keywordExpansionMap.put("생활", Arrays.asList("생활", "일상", "가족", "집", "음식", "요리", "쇼핑", "여행"));
        keywordExpansionMap.put("날씨", Arrays.asList("날씨", "기상", "비", "눈", "바람", "온도", "태풍", "폭우"));
        keywordExpansionMap.put("교통", Arrays.asList("교통", "버스", "지하철", "자동차", "도로", "운전", "사고"));
        keywordExpansionMap.put("음식", Arrays.asList("음식", "요리", "맛집", "레스토랑", "카페", "먹거리", "식당"));

        // === 국제 관련 ===
        keywordExpansionMap.put("국제",
            Arrays.asList("국제", "해외", "외국", "글로벌", "세계", "미국", "중국", "일본"));
        keywordExpansionMap.put("미국", Arrays.asList("미국", "워싱턴", "트럼프", "바이든", "달러", "NATO"));
        keywordExpansionMap.put("중국", Arrays.asList("중국", "베이징", "시진핑", "위안화", "홍콩", "대만"));

        log.info("키워드 확장 사전 초기화 완료: {}개 기본 키워드 (대폭 확장)", keywordExpansionMap.size());
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
        List<Interest> allInterests = interestRepository.findAll();
        Map<String, List<String>> interestKeywordsMap = new HashMap<>();

        for (Interest interest : allInterests) {
            List<String> originalKeywords = interestKeywordRepository.findKeywordsByInterestName(
                interest.getName());
            if (!originalKeywords.isEmpty()) {
                // 하드코딩된 사전으로 키워드 확장
                Set<String> expandedKeywords = expandKeywordsWithDictionary(originalKeywords);
                interestKeywordsMap.put(interest.getName(), new ArrayList<>(expandedKeywords));

                log.info("관심사 '{}': {}개 → {}개 키워드 확장", interest.getName(), originalKeywords.size(),
                    expandedKeywords.size());
                log.info("  원본: {}", originalKeywords);
                log.info("  확장: {}", expandedKeywords);
            }
        }

        return interestKeywordsMap;
    }


    private Set<String> expandKeywordsWithDictionary(List<String> originalKeywords) {
        Set<String> expandedKeywords = new HashSet<>(originalKeywords); // 원본 키워드 포함

        for (String keyword : originalKeywords) {
            if (keywordExpansionMap.containsKey(keyword)) {
                expandedKeywords.addAll(keywordExpansionMap.get(keyword));
                log.debug("키워드 '{}' 정확 매칭 확장: {}", keyword, keywordExpansionMap.get(keyword));
            } else {
                log.debug("키워드 '{}' - 사전에 없음, 원본 그대로 사용", keyword);
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
            // 사용자 키워드가 하드코딩 사전에 있는지 확인
            List<String> dictionaryMatchedKeywords = new ArrayList<>();
            List<String> unknownKeywords = new ArrayList<>();

            for (String keyword : keywords) {
                if (keywordExpansionMap.containsKey(keyword)) {
                    dictionaryMatchedKeywords.add(keyword);
                    log.info("하드코딩 사전 매칭 키워드: '{}'", keyword);
                } else {
                    unknownKeywords.add(keyword);
                    log.info("사전에 없는 키워드: '{}'", keyword);
                }
            }

            List<NewsArticle> allArticles = new ArrayList<>();

            if (!dictionaryMatchedKeywords.isEmpty()) {
                log.info("=== 특화 피드 수집: {}개 키워드로 20개 기사 수집 ===", dictionaryMatchedKeywords.size());
                List<NewsArticle> specializedArticles = newsCrawlingService.collectNewsForKeywords(
                    dictionaryMatchedKeywords, 20);
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

        // 활성화된 RSS 프로바이더만 필터링 (최대 3개로 제한)
        Map<String, NewsProviderProperties.ProviderConfig> enabledRssProviders = newsProviderProperties.getProviders()
            .entrySet().stream().filter(
                entry -> "rss".equals(entry.getValue().getType()) && entry.getValue().isEnabled())
            .limit(3) // 최대 3개 RSS 소스만 처리
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("활성화된 RSS 프로바이더: {}개 (최대 3개 제한)", enabledRssProviders.size());

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
                log.info("RSS '{}' 빠른 수집 중... (타임아웃 5초)", config.getName());

                long startTime = System.currentTimeMillis();

                // 키워드 없이 기사 수집 (빠른 처리)
                List<NewsArticleDto> dtos = rssProvider.fetchNews(config, new ArrayList<>());
                List<NewsArticle> articles = convertDtosToEntities(dtos);

                // 각 RSS당 최대 5개만 가져오기
                if (articles.size() > 5) {
                    articles = articles.subList(0, 5);
                }

                allRssArticles.addAll(articles);

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("RSS '{}': {}개 기사 수집 ({}ms)", config.getName(), articles.size(), elapsed);

                // 개별 RSS 처리가 5초 이상 걸리면 중단
                if (elapsed > 5000) {
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
        // 현재는 summary를 본문으로 사용 (실제로는 full content를 가져와야 함)
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
