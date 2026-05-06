package com.piotrcapecki.bakelivery.catalog.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MinioStorageServiceIT {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest")
            .withUserName("test")
            .withPassword("testtest");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("minio.endpoint", minio::getS3URL);
        r.add("minio.access-key", minio::getUserName);
        r.add("minio.secret-key", minio::getPassword);
        r.add("minio.bucket", () -> "bakelivery-catalog-test");
        r.add("minio.region", () -> "us-east-1");
        r.add("minio.presign-ttl-seconds", () -> "900");
    }

    @Autowired MinioStorageService storage;
    @Autowired S3Client s3;

    @Test
    void uploadDownloadDelete() throws Exception {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket("bakelivery-catalog-test").build());
        } catch (NoSuchBucketException e) {
            s3.createBucket(CreateBucketRequest.builder().bucket("bakelivery-catalog-test").build());
        }

        byte[] payload = "hello".getBytes();
        storage.upload("test/key.txt", "text/plain", payload.length, new ByteArrayInputStream(payload));
        String url = storage.presignedGetUrl("test/key.txt");
        assertThat(url).contains("test/key.txt");
        storage.delete("test/key.txt");
    }
}
