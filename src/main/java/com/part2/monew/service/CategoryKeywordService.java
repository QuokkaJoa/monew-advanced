package com.part2.monew.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class CategoryKeywordService {

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
        "IT", List.of(
            // AI 관련
            "AI", "ai", "인공지능", "머신러닝", "딥러닝", "ChatGPT", "생성AI", "LLM",
            // 기업명 - 기술
            "삼성", "Samsung", "애플", "Apple", "구글", "Google", "MS", "Microsoft", 
            "KT", "LG", "SK텔레콤", "네이버", "카카오", "메타", "Meta", "테슬라", "Tesla",
            // 기술 용어
            "스마트", "데이터", "빅데이터", "클라우드", "서버", "플랫폼", "API", "앱", "소프트웨어",
            "하드웨어", "반도체", "칩", "프로세서", "GPU", "CPU", "메모리", "SSD",
            "디지털", "온라인", "IoT", "5G", "6G", "블록체인", "암호화폐", "NFT", "메타버스",
            "로봇", "드론", "자율주행", "전기차", "배터리", "태양광", "신재생에너지",
            "게임", "e스포츠", "VR", "AR", "스타트업", "유니콘", "테크", "IT", "개발", "코딩",
            "해킹", "보안", "사이버", "랜섬웨어", "피싱", "DDoS"
        ),
        "경제", List.of(
            "경제", "투자", "주식", "코스피", "코스닥", "증시", "주가", "시가총액", "매출", "영업이익", "순이익",
            "기업", "회사", "대기업", "중소기업", "벤처", "스타트업", "IPO", "상장", "인수합병", "M&A",
            "은행", "금융", "대출", "금리", "기준금리", "통화정책", "인플레이션", "환율", "달러", "원화",
            "부동산", "아파트", "전세", "월세", "집값", "부동산시장", "건설", "분양",
            "펀드", "채권", "ETF", "파생상품", "선물", "옵션", "암호화폐", "비트코인", "이더리움",
            "GDP", "성장률", "무역", "수출", "수입", "무역수지", "경상수지", "고용", "실업률", "일자리"
        ),
        "정치", List.of(
            "정치", "정부", "청와대", "대통령", "총리", "국무총리", "장관", "국무회의",
            "국회", "의원", "국회의원", "여당", "야당", "더불어민주당", "국민의힘", "정의당", "개혁신당",
            "선거", "대선", "총선", "지방선거", "투표", "개표", "후보", "공약", "정책", "법안", "개정안",
            "외교", "북한", "중국", "미국", "일본", "러시아", "EU", "UN", "안보리", "G7", "G20",
            "통일", "비핵화", "제재", "협상", "정상회담", "외교부", "국방부", "통일부"
        ),
        "사회", List.of(
            "사회", "시민", "주민", "지역", "지방자치", "시장", "군수", "구청장", "도지사",
            "교육", "학교", "대학", "학생", "교사", "교육부", "입시", "수능", "교육과정",
            "복지", "연금", "의료보험", "건강보험", "사회보장", "기초생활수급", "장애인", "노인", "아동",
            "환경", "기후변화", "탄소중립", "미세먼지", "대기오염", "수질오염", "재활용", "친환경",
            "문화", "예술", "영화", "드라마", "K팝", "한류", "관광", "축제", "공연", "전시",
            "종교", "기독교", "불교", "천주교", "이슬람", "교회", "절", "성당", "모스크",
            "사건사고", "화재", "교통사고", "자연재해", "지진", "태풍", "홍수", "산사태"
        ),
        "스포츠", List.of(
            "스포츠", "운동", "경기", "선수", "감독", "코치", "팀", "클럽", "리그", "시즌", "우승", "준우승",
            "축구", "야구", "농구", "배구", "테니스", "골프", "수영", "육상", "체조", "태권도", "유도", "검도",
            "올림픽", "월드컵", "아시안게임", "패럴림픽", "FIFA", "IOC", "KFA", "KBO", "KBL", "V리그",
            "프리미어리그", "라리가", "세리에A", "분데스리가", "NBA", "MLB", "NFL", "PGA",
            "손흥민", "이강인", "박지성", "김연아", "박세리", "추신수", "류현진", "이승엽",
            "토트넘", "맨유", "맨시티", "리버풀", "첼시", "아스날", "바르셀로나", "레알마드리드", "파리생제르맹"
        )
    );

    public String inferCategoryFromContent(String title, String content) {
        String fullText = (title != null ? title : "") + " " + (content != null ? content : "");
        
        // 각 카테고리별로 키워드 매칭
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> keywordList = entry.getValue();
            
            for (String keyword : keywordList) {
                // 대소문자 구분 없이 정확한 단어 매칭 (정규식 사용)
                String regex = "\\b" + java.util.regex.Pattern.quote(keyword) + "\\b";
                if (java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(fullText).find()) {
                    log.debug("카테고리 '{}' 매칭 - 키워드: '{}'", category, keyword);
                    return category;
                }

                if (fullText.toLowerCase().contains(keyword.toLowerCase())) {
                    log.debug("카테고리 '{}' 매칭 (contains) - 키워드: '{}'", category, keyword);
                    return category;
                }
            }
        }
        
        return "기타";
    }

    public List<String> getKeywordsForCategory(String category) {
        return CATEGORY_KEYWORDS.getOrDefault(category, List.of());
    }

    public List<String> getAllCategories() {
        return List.copyOf(CATEGORY_KEYWORDS.keySet());
    }

    public Map<String, List<String>> getAllCategoryKeywords() {
        return Map.copyOf(CATEGORY_KEYWORDS);
    }


    public boolean isKeywordInCategory(String keyword, String category) {
        List<String> keywords = CATEGORY_KEYWORDS.get(category);
        if (keywords == null) return false;
        
        return keywords.stream()
            .anyMatch(k -> k.equalsIgnoreCase(keyword));
    }
} 