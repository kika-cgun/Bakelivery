package com.piotrcapecki.bakelivery.routing.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RabbitConfigTest {
    @MockitoBean RedissonClient redissonClient;
    @MockitoBean RabbitTemplate rabbitTemplate;

    @Autowired TopicExchange bakeliveryEvents;
    @Autowired DirectExchange routingDirect;
    @Autowired Queue routingOptimizeQueue;
    @Autowired Queue routingDispatchAssignedQueue;

    @Test
    void exchangesAndQueuesExist() {
        assertThat(bakeliveryEvents.getName()).isEqualTo("bakelivery.events");
        assertThat(routingOptimizeQueue.getName()).isEqualTo("routing.optimize");
        assertThat(routingDispatchAssignedQueue.getName()).isEqualTo("routing.dispatch_assigned");
    }
}
