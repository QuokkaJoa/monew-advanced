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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3LogUploader {
    private static final Logger logger = LoggerFactory.getLogger(S3LogUploader.class);
    private S3Client s3Client;
    private final String s3LogPrefix = "logs/";
    private final String logDirPath = "./.logs";
    
    // 뉴스 백업 관련 상수
    private static final String BACKUP_FILE_PREFIX = "backups/news/";
    private static final String BACKUP_FILE_SUFFIX = ".json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
    

    public String getBackupFileKey(LocalDate date) {
        return BACKUP_FILE_PREFIX + date.format(DATE_FORMATTER) + BACKUP_FILE_SUFFIX;
    }

    public void uploadNewsBackup(byte[] backupData, String s3Key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType("application/json") 
                .build();
        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(backupData));
            logger.info("뉴스 백업 S3 업로드 성공: {}", s3Key);
        } catch (S3Exception e) {
            logger.error("S3 뉴스 백업 업로드 실패: {}", s3Key, e);
            String errorMessage = String.format(
                "❌ 뉴스 백업 S3 업로드 실패\n" +
                "S3 Key: '%s'\n" +
                "버킷: '%s'\n" +
                "오류: %s",
                s3Key, bucket, e.getMessage()
            );
            slackNotificationService.sendNotification(errorMessage);
            throw new RuntimeException("S3 백업 업로드 실패: " + s3Key, e);
        }
    }

    public InputStream downloadNewsBackup(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            return s3Object;
        } catch (S3Exception e) {
            logger.error("S3 뉴스 백업 다운로드 실패: {}", s3Key, e);
            if (e.statusCode() == 404) {
                 logger.warn("S3에 해당 키의 백업 파일이 없습니다: {}", s3Key);
                 return null;
            }
            throw new RuntimeException("S3 백업 다운로드 실패: " + s3Key, e);
        }
    }

    public String getLatestBackupKey() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        LocalDate yesterday = today.minusDays(1);
        
        // 오늘 백업 파일이 있는지 먼저 확인
        String todayKey = getBackupFileKey(today);
        try {
            GetObjectRequest todayRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(todayKey)
                    .build();
            s3Client.getObject(todayRequest).close(); // 파일 존재 확인
            logger.info("오늘({}) 백업 파일 찾음: {}", today, todayKey);
            return todayKey;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                logger.info("오늘({}) 백업 파일이 없음. 어제({}) 백업 파일로 대체", today, yesterday);
            } else {
                logger.error("오늘 백업 파일 확인 중 오류 발생: {}", e.getMessage());
            }
            return getBackupFileKey(yesterday);
        } catch (Exception e) {
            logger.error("오늘 백업 파일 확인 중 예상치 못한 오류: {}", e.getMessage());
            return getBackupFileKey(yesterday);
        }
    }
}
