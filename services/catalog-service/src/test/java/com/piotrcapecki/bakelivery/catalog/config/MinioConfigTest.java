package com.piotrcapecki.bakelivery.catalog.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MinioConfigTest {
    @Autowired(required = false) S3Client s3Client;
    @Autowired(required = false) S3Presigner s3Presigner;

    @Test
    void s3BeansAreWired() {
        assertThat(s3Client).isNotNull();
        assertThat(s3Presigner).isNotNull();
    }
}
