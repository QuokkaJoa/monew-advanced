package com.part2.monew.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 간단한 추출적 요약을 수행하는 유틸 클래스
 * 문장 길이와 키워드 빈도를 기반으로 중요한 문장들을 추출합니다.
 */
@Slf4j
public class TextRankSummarizer {

    private static final int DEFAULT_TOP_SENTENCES = 3;
    private static final int MIN_TEXT_LENGTH = 100;
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]\\s+");
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w{2,}\\b");

    /**
     * rawText(기사 전체 본문)를 받아서 상위 K개 문장을 추출해 하나의 요약문으로 이어붙여 반환한다.
     * @param rawText 기사 본문 문자열
     * @param topK 뽑아낼 문장 개수 (예: 3개 문장)
     * @return 문장들을 이어붙인 요약문. rawText가 너무 짧거나 문제가 있을 경우 원본 텍스트의 일부 또는 빈 문자열 리턴
     */
    public static String summarize(String rawText, int topK) {
        if (rawText == null || rawText.trim().isEmpty()) {
            log.warn("텍스트가 null이거나 비어있어 요약할 수 없습니다.");
            return "";
        }

        String cleanText = rawText.trim()
                .replaceAll("\\s+", " ")  // 여러 공백을 하나로
                .replaceAll("[\\r\\n]+", " ");  // 줄바꿈을 공백으로
        
        // 텍스트가 너무 짧으면 원본 반환
        if (cleanText.length() < MIN_TEXT_LENGTH) {
            log.debug("텍스트가 너무 짧아서({} chars) 요약하지 않고 원본을 반환합니다.", cleanText.length());
            return cleanText;
        }

        try {
            // 문장 분리
            List<String> sentences = splitIntoSentences(cleanText);
            
            if (sentences.isEmpty()) {
                log.warn("문장을 추출할 수 없어 원본 텍스트의 일부를 반환합니다.");
                return cleanText.length() > 200 ? cleanText.substring(0, 200) + "..." : cleanText;
            }
            
            // 전체 문장 수보다 많은 문장을 요청하지 않도록 조정
            int actualTopK = Math.min(topK, sentences.size());
            
            // 각 문장에 점수 부여
            List<ScoredSentence> scoredSentences = scoreSentences(sentences);
            
            // 상위 K개 문장 선택 (원래 순서 유지)
            List<String> topSentences = scoredSentences.stream()
                    .sorted((a, b) -> Double.compare(b.score, a.score))  // 점수 높은 순
                    .limit(actualTopK)
                    .sorted((a, b) -> Integer.compare(a.originalIndex, b.originalIndex))  // 원래 순서 복원
                    .map(s -> s.text)
                    .collect(Collectors.toList());

            String summary = String.join(" ", topSentences);

            if (summary.trim().isEmpty()) {
                log.warn("요약 결과가 비어있어 원본 텍스트의 일부를 반환합니다.");
                return cleanText.length() > 200 ? cleanText.substring(0, 200) + "..." : cleanText;
            }
            
            log.debug("요약 완료: {} chars -> {} chars", cleanText.length(), summary.length());
            return summary;
            
        } catch (Exception e) {
            log.error("텍스트 요약 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시 원본 텍스트의 일부라도 반환
            return cleanText.length() > 200 ? cleanText.substring(0, 200) + "..." : cleanText;
        }
    }

    /**
     * 기본값(3개 문장)으로 요약을 수행한다.
     * @param rawText 기사 본문 문자열
     * @return 요약문
     */
    public static String summarize(String rawText) {
        return summarize(rawText, DEFAULT_TOP_SENTENCES);
    }
    
    /**
     * 텍스트를 문장으로 분리
     */
    private static List<String> splitIntoSentences(String text) {
        return Arrays.stream(SENTENCE_PATTERN.split(text))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && s.length() > 10)  // 너무 짧은 문장 제외
                .collect(Collectors.toList());
    }
    
    /**
     * 각 문장에 점수 부여 (문장 길이, 단어 빈도 등 고려)
     */
    private static List<ScoredSentence> scoreSentences(List<String> sentences) {
        // 전체 단어 빈도 계산
        Map<String, Integer> wordFreq = calculateWordFrequency(sentences);
        
        List<ScoredSentence> scoredSentences = new ArrayList<>();
        
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double score = calculateSentenceScore(sentence, wordFreq);
            scoredSentences.add(new ScoredSentence(sentence, score, i));
        }
        
        return scoredSentences;
    }
    
    /**
     * 전체 텍스트에서 단어 빈도 계산
     */
    private static Map<String, Integer> calculateWordFrequency(List<String> sentences) {
        Map<String, Integer> wordFreq = new HashMap<>();
        
        for (String sentence : sentences) {
            List<String> words = extractWords(sentence.toLowerCase());
            for (String word : words) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }
        
        return wordFreq;
    }
    
    /**
     * 문장에서 단어 추출
     */
    private static List<String> extractWords(String text) {
        return WORD_PATTERN.matcher(text)
                .results()
                .map(match -> match.group())
                .filter(word -> word.length() > 1)
                .collect(Collectors.toList());
    }
    
    /**
     * 문장 점수 계산 (단어 빈도 기반)
     */
    private static double calculateSentenceScore(String sentence, Map<String, Integer> wordFreq) {
        List<String> words = extractWords(sentence.toLowerCase());
        
        if (words.isEmpty()) {
            return 0.0;
        }
        
        double totalScore = words.stream()
                .mapToDouble(word -> wordFreq.getOrDefault(word, 0))
                .sum();
        
        // 문장 길이로 정규화 (너무 긴 문장에 불이익)
        double lengthPenalty = Math.min(1.0, 50.0 / words.size());
        
        return (totalScore / words.size()) * lengthPenalty;
    }
    
    /**
     * 점수가 매겨진 문장을 나타내는 내부 클래스
     */
    private static class ScoredSentence {
        final String text;
        final double score;
        final int originalIndex;
        
        ScoredSentence(String text, double score, int originalIndex) {
            this.text = text;
            this.score = score;
            this.originalIndex = originalIndex;
        }
    }
} 