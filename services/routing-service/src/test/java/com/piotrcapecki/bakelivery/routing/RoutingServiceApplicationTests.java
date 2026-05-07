package com.piotrcapecki.bakelivery.routing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@SpringBootTest
@ActiveProfiles("test")
class RoutingServiceApplicationTests {
    @MockitoBean RedissonClient redissonClient;
    @MockitoBean RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {}
}
