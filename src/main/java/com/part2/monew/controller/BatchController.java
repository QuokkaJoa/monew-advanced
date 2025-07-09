package com.part2.monew.controller;

import com.part2.monew.service.SpringBatch;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/batch")
public class BatchController {
    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);
    private final SpringBatch springBatch;

    /** 뉴스 수집 배치 수동 트리거 */
    @PostMapping("/news-collect")
    public ResponseEntity<Void> triggerNewsCollect() {
        logger.info("[BatchController] 수동 뉴스 수집 배치 시작");
        springBatch.executeNewsCollectionBatch();
        return ResponseEntity.ok().build();
    }
}
