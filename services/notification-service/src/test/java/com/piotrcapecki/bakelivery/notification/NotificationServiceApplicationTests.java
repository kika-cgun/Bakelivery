package com.piotrcapecki.bakelivery.notification;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {
    @MockitoBean ConnectionFactory connectionFactory;
    @MockitoBean RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {}
}
