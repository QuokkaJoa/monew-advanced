package com.part2.monew.service;

import com.part2.monew.config.BatchConfig;
import com.part2.monew.entity.NewsArticle;
import com.part2.monew.service.impl.NewsArticleService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SpringBatch {
    // 나중에 배치로 수정함
    private static final Logger logger = LoggerFactory.getLogger(SpringBatch.class);
    
    private final BatchConfig batchConfig;
    private final SimpleNewsCollectionService newsCollectionService;
    private final NewsBackupS3Manager newsBackupS3Manager;
    private final NewsArticleService newsArticleService;

    public SpringBatch(BatchConfig batchConfig, 
                      SimpleNewsCollectionService newsCollectionService,
                      NewsBackupS3Manager newsBackupS3Manager,
                      NewsArticleService newsArticleService) {
        this.batchConfig = batchConfig;
        this.newsCollectionService = newsCollectionService;
        this.newsBackupS3Manager = newsBackupS3Manager;
        this.newsArticleService = newsArticleService;
        
        logger.info("SpringBatch 초기화 완료 - 스케줄링 전용");
    }

    @Scheduled(cron = "0 0 0/1 * * *")
    public void executeNewsCollectionBatch() {
        if (!batchConfig.isEnabled()) {
            logger.info("뉴스 수집 배치가 비활성화되어 있습니다.");
            return;
        }

        try {
            List<NewsArticle> collectedArticles = newsCollectionService.collectNewsWithSimpleKeywordMatching();
            
            logger.info("=== 뉴스 수집 배치 완료: 총 {}개 기사 수집 ===", collectedArticles.size());
            
        } catch (Exception e) {
            logger.error("뉴스 수집 배치 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 매일 자정에 백업
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void executeDailyBackupBatch() {
        if (!batchConfig.isEnabled()) {
            logger.info("백업 배치가 비활성화되어 있습니다.");
            return;
        }

        logger.info("=== 일일 백업 배치 시작 ===");
        
        try {
            performDailyNewsBackup();
            logger.info("=== 일일 백업 배치 완료 ===");
            
        } catch (Exception e) {
            logger.error("일일 백업 배치 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private void performDailyNewsBackup() {
        try {
            logger.info("=== 일일 뉴스 백업 시작 ===");
            
            LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
            logger.info("백업 대상 날짜: {}", today);
            

            newsArticleService.backupDataByDate(today);
            
            logger.info("=== 일일 뉴스 백업 완료 ===");
            
        } catch (Exception e) {
            logger.error("일일 뉴스 백업 실패: {}", e.getMessage(), e);
        }
    }
}
