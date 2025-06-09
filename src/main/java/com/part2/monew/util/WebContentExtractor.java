package com.part2.monew.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 웹 페이지에서 기사 본문을 추출하는 유틸리티 클래스
 */
@Slf4j
public class WebContentExtractor {

    private static final int TIMEOUT_SECONDS = 10;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    
    // 한국 언론사별 본문 선택자들
    private static final String[] CONTENT_SELECTORS = {
        // 일반적인 선택자들
        "article",
        ".article-content",
        ".article-body", 
        ".news-content",
        ".post-content",
        ".entry-content",
        "#article-body",
        "#newsContent",
        
        // 한국 언론사별 선택자
        ".article_body",  // 조선일보
        ".news_article",  // 한경
        ".article_txt",   // 연합뉴스
        ".news_text",     // 네이버 뉴스
        ".read_body",     // 다음 뉴스
        ".article-view__content", // 일부 언론사
        
        // 백업 선택자
        "p",
        "div"
    };

    public static String extractContent(String url) {
        if (url == null || url.trim().isEmpty()) {
            log.warn("URL이 null이거나 비어있습니다.");
            return "";
        }

        try {
            log.debug("기사 본문 추출 시작: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
                    .get();
            
            String content = extractContentFromDocument(doc);
            
            if (content.trim().isEmpty()) {
                log.warn("URL에서 본문을 추출할 수 없습니다: {}", url);
                return "";
            }
            
            log.debug("본문 추출 완료: {} chars", content.length());
            return content;
            
        } catch (IOException e) {
            log.warn("URL 접근 실패: {} - {}", url, e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("본문 추출 중 오류: {} - {}", url, e.getMessage(), e);
            return "";
        }
    }
    private static String extractContentFromDocument(Document doc) {
        // 순서대로 선택자를 시도하여 가장 적절한 내용 찾기
        for (String selector : CONTENT_SELECTORS) {
            Elements elements = doc.select(selector);
            
            for (Element element : elements) {
                String text = element.text();
                
                // 유효한 본문으로 판단하는 기준
                if (isValidContent(text)) {
                    return cleanText(text);
                }
            }
        }
        
        // 모든 선택자가 실패한 경우 body 전체에서 추출
        String bodyText = doc.body().text();
        return cleanText(bodyText);
    }

    /**
     * 텍스트가 유효한 기사 본문인지 판단합니다.
     */
    private static boolean isValidContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // 너무 짧거나 너무 긴 텍스트 제외
        int length = text.trim().length();
        if (length < 100 || length > 50000) {
            return false;
        }
        
        // 문장의 개수 확인 (적절한 기사라면 여러 문장이 있을 것)
        long sentenceCount = text.chars()
                .filter(ch -> ch == '.' || ch == '!' || ch == '?')
                .count();
        
        return sentenceCount >= 3; // 최소 3개 문장
    }

    /**
     * 텍스트를 정리합니다.
     */
    private static String cleanText(String text) {
        if (text == null) {
            return "";
        }
        
        return text
                .replaceAll("\\s+", " ")  // 여러 공백을 하나로
                .replaceAll("[\\r\\n]+", " ")  // 줄바꿈을 공백으로
                .trim();
    }
} 