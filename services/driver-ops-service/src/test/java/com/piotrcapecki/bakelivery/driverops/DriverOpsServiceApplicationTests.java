package com.piotrcapecki.bakelivery.driverops;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@SpringBootTest
@ActiveProfiles("test")
class DriverOpsServiceApplicationTests {

    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean S3Client s3Client;
    @MockitoBean S3Presigner s3Presigner;

    @Test
    void contextLoads() {}
}
