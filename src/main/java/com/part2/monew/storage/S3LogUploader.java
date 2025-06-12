package com.part2.monew.storage;

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

@Component
public class S3LogUploader {
    private S3Client s3Client;
    private final String s3LogPrefix = "logs/";
    private final String logDirPath = "./.logs";

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

    @Scheduled(cron = "0 0 19 * * *")
    public void uploadAllLogs() {
        System.out.println("S3 자동 업로드 시작: " + LocalDateTime.now());
        File logDir = new File(logDirPath);
        if (!logDir.exists() || !logDir.isDirectory()) {
            System.out.println("로그 디렉토리가 존재하지 않음: " + logDirPath);
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
            } catch (Exception e) {
                System.err.println("S3 업로드 실패: " + file.getName());
                e.printStackTrace();
            }
        }
    }
}
