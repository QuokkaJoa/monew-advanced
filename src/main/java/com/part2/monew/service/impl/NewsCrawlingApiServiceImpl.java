package com.part2.monew.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.repository.NewsArticleRepository;
import com.part2.monew.service.newsprovider.NewsCrawlingService;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Service
public class NewsCrawlingApiServiceImpl implements NewsCrawlingService {

    private static final Logger logger = LoggerFactory.getLogger(NewsCrawlingApiServiceImpl.class);
    private static final String SOURCE_NAME = "Naver News";

    @Value("${monew.news-providers.naver-api.client-id}")
    private String clientId;

    @Value("${monew.news-providers.naver-api.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NewsArticleRepository newsArticleRepository;

    public NewsCrawlingApiServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper, 
                                    NewsArticleRepository newsArticleRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.newsArticleRepository = newsArticleRepository;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<NewsArticle> searchNews(String query, int limit) {
        return searchNews(query, limit, 1, "date");
    }

    @Override
    public List<NewsArticle> searchAndSaveNews(String query, int limit) {
        return searchAndSaveNews(query, limit, 1, "date");
    }

    @Transactional
    public List<NewsArticle> collectNewsForKeywords(List<String> keywords, int limitPerKeyword) {
        List<NewsArticle> allSavedArticles = new ArrayList<>();
        
        for (String keyword : keywords) {
            try {
                List<NewsArticle> savedArticles = searchAndSaveNews(keyword, limitPerKeyword);
                allSavedArticles.addAll(savedArticles);
            } catch (Exception e) {
            }
        }
        
        return allSavedArticles;
    }

    public List<NewsArticle> searchNews(String query, int display, int start, String sort) {
        List<NewsArticle> articles = fetchNewsFromNaver(query, display, start, sort);
        return removeDuplicateArticles(articles);
    }

    @Transactional
    public List<NewsArticle> searchAndSaveNews(String query, int display, int start, String sort) {
        List<NewsArticle> articles = fetchNewsFromNaver(query, display, start, sort);
        List<NewsArticle> newArticles = removeDuplicateArticles(articles);
        
        if (!newArticles.isEmpty()) {
            List<NewsArticle> savedArticles = newsArticleRepository.saveAll(newArticles);
            return savedArticles;
        }
        
        return new ArrayList<>();
    }

    private List<NewsArticle> fetchNewsFromNaver(String query, int display, int start, String sort) {
        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            logger.error("검색어 인코딩 실패");
            throw new RuntimeException("검색어 인코딩 실패", e);
        }

        String apiUrl = "https://openapi.naver.com/v1/search/news.json";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
            .queryParam("query", encodedQuery)
            .queryParam("display", Math.max(1, Math.min(display, 100)))
            .queryParam("start", Math.max(1, Math.min(start, 1000)))
            .queryParam("sort", (sort != null && (sort.equals("sim") || sort.equals("date"))) ? sort : "date");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseNewsSearchResponse(response.getBody());
            } else {
                logger.warn("네이버 API 응답 실패");
                return List.of();
            }
        } catch (HttpClientErrorException e) {
            logger.error("네이버 뉴스 API 요청 실패");
            throw new RuntimeException("네이버 뉴스 API 요청 실패");
        } catch (Exception e) {
            logger.error("네이버 뉴스 API 요청 중 오류 발생", e);
            throw new RuntimeException("네이버 뉴스 API 요청 중 오류 발생", e);
        }
    }

    private List<NewsArticle> parseNewsSearchResponse(String responseBody) {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode itemsNode = rootNode.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    String title = itemNode.path("title").asText();
                    String sourceUrl = itemNode.path("link").asText(null);
                    String description = itemNode.path("description").asText();
                    String pubDateStr = itemNode.path("pubDate").asText(null);
                    Timestamp parsedTimestamp = parseNaverDateString(pubDateStr);

                    NewsArticle article = NewsArticle.builder()
                        .sourceIn("Naver News")
                        .sourceUrl(truncateString(sourceUrl, 2048))
                        .title(truncateString(stripHtml(title), 500))
                        .publishedDate(parsedTimestamp)
                        .summary(stripHtml(description))
                        .viewCount(0L)
                        .build();
                    articles.add(article);
                }
            }
        } catch (Exception e) {
            logger.error("네이버 뉴스 API 응답 파싱 중 오류 발생", e);
            return new ArrayList<>();
        }
        return articles;
    }

    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    private Timestamp parseNaverDateString(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, formatter);
            return Timestamp.valueOf(zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        } catch (Exception e) {
            return null;
        }
    }

    // 중복 기사 제거 
    private List<NewsArticle> removeDuplicateArticles(List<NewsArticle> articles) {
        if (articles.isEmpty()) {
            return new ArrayList<>();
        }

        // sourceUrl이 null이거나 빈 문자열인 기사 필터링
        List<NewsArticle> validArticles = articles.stream()
            .filter(article -> article.getSourceUrl() != null && !article.getSourceUrl().trim().isEmpty())
            .collect(Collectors.toList());

        if (validArticles.isEmpty()) {
            return new ArrayList<>();
        }

        // DB에 이미 존재하는 sourceUrl들 조회
        List<String> sourceUrls = validArticles.stream()
            .map(NewsArticle::getSourceUrl)
            .collect(Collectors.toList());

        List<String> existingUrls = newsArticleRepository.findExistingSourceUrls(sourceUrls);

        // DB에 존재하지 않는 기사들만 반환
        List<NewsArticle> newArticles = validArticles.stream()
            .filter(article -> !existingUrls.contains(article.getSourceUrl()))
            .collect(Collectors.toList());

        logger.debug("중복체크 결과 - 전체: {}개, 유효한 URL: {}개, DB 중복제거 후: {}개", 
                    articles.size(), validArticles.size(), newArticles.size());

        return newArticles;
    }

    private String truncateString(String str, int maxLength) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}