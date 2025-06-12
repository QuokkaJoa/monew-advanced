package com.part2.monew.util;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SlackNotificationService {

  @Value("${monew.cloud.slack.webhook-url}")
  private String slackWebhookUrl;

  private final RestTemplate restTemplate;

  public SlackNotificationService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public void sendNotification(String message) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> payload = new HashMap<>();
    payload.put("text", message);

    HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

    try {
      restTemplate.postForEntity(slackWebhookUrl, request, String.class);
      log.info("Slack 알림 성공적으로 전송");
    } catch (Exception e) {
        log.error("Slack 알림 전송 중 오류 발생: " + e.getMessage());
    }
  }
}
