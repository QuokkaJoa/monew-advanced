package com.part2.monew.storage;

import com.part2.monew.util.SlackNotificationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.time.LocalDateTime;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3LogUploader {
    private S3Client s3Client;
    private final String s3LogPrefix = "logs/";
    private final String logDirPath = "./.logs";

    private final SlackNotificationService slackNotificationService;

    public S3LogUploader(SlackNotificationService slackNotificationService) {
        this.slackNotificationService = slackNotificationService;
    }

    @Value("${monew.storage.s3.accessKeyId}")
    private String accessKey;

    @Value("${monew.storage.s3.secretAccessKey}")
    private String secretKey;

    @Value("${monew.storage.s3.bucket}")
    private String bucket;

    @PostConstruct
    public void S3LogUploader() {
        this.s3Client = S3Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKey,
                                        secretKey
                                )
                        )
                )
                .build();
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void uploadAllLogs() {
        System.out.println("S3 자동 업로드 시작: " + LocalDateTime.now());
        File logDir = new File(logDirPath);
        if (!logDir.exists() || !logDir.isDirectory()) {
            String errorMessage = String.format("S3 업로드 경고: 로그 디렉토리 '%s'가 존재하지 않거나 디렉토리가 아닙니다.",
                logDirPath);
            slackNotificationService.sendNotification(errorMessage);
            return;
        }
        File[] logs = logDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logs == null) return;

        for (File file : logs) {
            String s3Key = s3LogPrefix + file.getName();
            try {
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(s3Key)
                                .build(),
                        file.toPath()
                );
                System.out.println(file.getName() + " → S3 업로드 성공");
            } catch (S3Exception e) {
                String errorMessage = String.format(
                    "❌ S3 업로드 실패 알림 (AWS S3 오류)\n" +
                        "파일: '%s' (S3 Key: '%s')\n" +
                        "버킷: '%s\n" +
                        "오류 코드: '%s'\n" +
                        "오류 메시지: '%s'",
                    file.getName(), s3Key, bucket, e.awsErrorDetails().errorCode(),
                    e.awsErrorDetails().errorMessage()
                );
                System.err.println(errorMessage);
                slackNotificationService.sendNotification(errorMessage);
            } catch (Exception e) {
                String errorMessage = String.format(
                    "❌ S3 업로드 실패 알림 (예상치 못한 오류)\n" +
                        "파일: '%s' (S3 Key: '%s')\n" +
                        "버킷: '%s\n" +
                        "오류 코드: '%s'\n" +
                        "오류 메시지: '%s'",
                    file.getName(), s3Key, bucket, e.getClass().getSimpleName(),e.getMessage()
                );
                System.err.println(errorMessage);
                slackNotificationService.sendNotification(errorMessage);
            }
        }
    }
}
