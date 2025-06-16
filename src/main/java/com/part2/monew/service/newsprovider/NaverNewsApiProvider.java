package com.part2.monew.service.newsprovider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.part2.monew.config.NewsProviderProperties.ProviderConfig;
import com.part2.monew.dto.request.NewsArticleDto;
import com.part2.monew.service.CategoryKeywordService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class NaverNewsApiProvider implements NewsProvider {

    private final CategoryKeywordService categoryKeywordService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderKey() {
        return "Naver News API";
    }

    @Override
    public List<NewsArticleDto> fetchNews(ProviderConfig config, List<String> keywords) {
        List<NewsArticleDto> allNews = new ArrayList<>();

        for (String keyword : keywords) {
            try {
                List<NewsArticleDto> keywordNews = fetchNewsByKeyword(config, keyword);

                List<NewsArticleDto> filteredNews = applyCategoryBasedFilter(keywordNews, keywords,
                    keyword);

                allNews.addAll(filteredNews);
                log.debug("키워드 '{}' 기사 제목들: {}", keyword,
                    filteredNews.stream().map(NewsArticleDto::getTitle).limit(3).toList());
            } catch (Exception e) {
                log.error("네이버 API 키워드 '{}' 수집 실패: {}", keyword, e.getMessage());
            }
        }

        log.info("네이버 API 뉴스 수집 완료 - 총 {}건", allNews.size());
        return allNews;
    }

    private List<NewsArticleDto> fetchNewsByKeyword(ProviderConfig config, String keyword) {
        try {
            // API URL 구성
            String url = UriComponentsBuilder.fromUriString(config.getApiUrl())
                .queryParam("query", keyword).queryParam("display", config.getDefaultDisplay())
                .queryParam("sort", config.getDefaultSort()).build().toUriString();

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", config.getClientId());
            headers.set("X-Naver-Client-Secret", config.getClientSecret());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                String.class);

            return parseNaverApiResponse(response.getBody(), keyword);

        } catch (Exception e) {
            log.error("네이버 API 호출 실패 - 키워드: {}, 오류: {}", keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsArticleDto> parseNaverApiResponse(String jsonResponse, String keyword) {
        List<NewsArticleDto> newsList = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode items = rootNode.path("items");

            for (JsonNode item : items) {
                NewsArticleDto news = NewsArticleDto.builder().providerName(getProviderKey())
                    .title(cleanHtmlTags(item.path("title").asText()))
                    .originalLink(item.path("originallink").asText())
                    .publishedDate(parsePublishedDate(item.path("pubDate").asText()))
                    .summaryOrContent(cleanHtmlTags(item.path("description").asText()))
                    .guid(item.path("link").asText()).thumbnailUrl(null).build();

                newsList.add(news);
            }

        } catch (Exception e) {
            log.error("네이버 API 응답 파싱 실패 - 키워드: {}, 오류: {}", keyword, e.getMessage());
        }

        return newsList;
    }

    private String cleanHtmlTags(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("<[^>]*>", "").trim();
    }

    private List<NewsArticleDto> applyAdditionalKeywordFilter(List<NewsArticleDto> newsList,
        List<String> allKeywords) {
        return newsList.stream().filter(news -> containsAnyKeyword(news, allKeywords)).toList();
    }

    private boolean containsAnyKeyword(NewsArticleDto news, List<String> keywords) {
        String title = news.getTitle() != null ? news.getTitle() : "";
        String content = news.getSummaryOrContent() != null ? news.getSummaryOrContent() : "";

        log.debug("네이버 API 기사 제목: {}", title);
        log.debug("네이버 API 기사 내용: {}",
            content.length() > 100 ? content.substring(0, 100) + "..." : content);

        // 제목이나 내용에 키워드 중 하나라도 포함되어 있으면 true
        for (String keyword : keywords) {
            if (title.contains(keyword) || content.contains(keyword)) {
                log.debug("네이버 API 키워드 '{}' 매칭됨! (제목: {}, 내용: {})", keyword,
                    title.contains(keyword), content.contains(keyword));
                return true;
            }
        }
        log.debug("네이버 API 키워드 매칭 안됨 - 키워드: {}", keywords);
        return false;
    }

    private Timestamp parsePublishedDate(String pubDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, formatter);
            LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
            
            return Timestamp.valueOf(localDateTime);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 현재 시간 사용", pubDate);
            return new Timestamp(System.currentTimeMillis());
        }
    }

    private List<NewsArticleDto> applyCategoryBasedFilter(List<NewsArticleDto> articles,
        List<String> allKeywords, String searchKeyword) {
        List<NewsArticleDto> filteredArticles = new ArrayList<>();

        for (NewsArticleDto article : articles) {
            String title = article.getTitle() != null ? article.getTitle() : "";
            String content =
                article.getSummaryOrContent() != null ? article.getSummaryOrContent() : "";

            // 제목과 내용에서 카테고리 추론
            String inferredCategory = categoryKeywordService.inferCategoryFromContent(title,
                content);

            boolean shouldInclude = false;
            String matchReason = "";

            // 1. 직접 키워드 매칭 (원래 검색 키워드)
            if (title.toLowerCase().contains(searchKeyword.toLowerCase()) || content.toLowerCase()
                .contains(searchKeyword.toLowerCase())) {
                shouldInclude = true;
                matchReason = "검색키워드 직접매칭: " + searchKeyword;
            }

            // 2. 추론된 카테고리가 관심사 목록에 있는지 확인
            if (!shouldInclude) {
                for (String keyword : allKeywords) {
                    if (inferredCategory.equalsIgnoreCase(keyword)) {
                        shouldInclude = true;
                        matchReason = "카테고리매칭: " + inferredCategory + " = " + keyword;
                        break;
                    }
                }
            }

            // 3. 관심사 키워드가 제목/내용에 직접 포함되어 있는지 확인
            if (!shouldInclude) {
                for (String keyword : allKeywords) {
                    if (title.toLowerCase().contains(keyword.toLowerCase()) || content.toLowerCase()
                        .contains(keyword.toLowerCase())) {
                        shouldInclude = true;
                        matchReason = "키워드직접매칭: " + keyword;
                        break;
                    }
                }
            }

            // 4. IT 카테고리의 경우 더 광범위하게 포함 (현재 관심사에 IT가 있다면)
            if (!shouldInclude && allKeywords.stream().anyMatch(k -> k.equalsIgnoreCase("IT"))) {
                if ("IT".equals(inferredCategory)) {
                    shouldInclude = true;
                    matchReason = "IT카테고리 광범위매칭";
                }
            }

            if (shouldInclude) {
                log.debug("✅ 포함됨 - 이유: {}", matchReason);
                filteredArticles.add(article);
            } else {
                log.debug("❌ 제외됨 - 매칭되는 키워드 없음");
            }
        }

        return filteredArticles;
    }


}
