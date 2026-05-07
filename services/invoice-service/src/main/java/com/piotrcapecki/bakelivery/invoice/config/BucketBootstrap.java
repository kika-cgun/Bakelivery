package com.piotrcapecki.bakelivery.invoice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class BucketBootstrap {

    @Value("${minio.bucket}") private String bucket;

    @Bean
    public ApplicationRunner ensureBucket(S3Client s3) {
        return args -> {
            try {
                s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                log.info("MinIO bucket '{}' already exists", bucket);
            } catch (NoSuchBucketException e) {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created MinIO bucket '{}'", bucket);
            }
        };
    }
}
