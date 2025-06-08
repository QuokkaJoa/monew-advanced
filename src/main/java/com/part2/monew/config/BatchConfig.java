package com.part2.monew.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "monew.batch.news-collection")
public class BatchConfig {
    
    private boolean enabled = true;
    private String cron = "0 0 * * * *";  // 매시간 정각 기본값
    private List<String> keywords;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getCron() {
        return cron;
    }
    
    public void setCron(String cron) {
        this.cron = cron;
    }
    
    public List<String> getKeywords() {
        return keywords;
    }
    
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
} 