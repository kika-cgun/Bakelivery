package com.piotrcapecki.bakelivery.messaging.repository;

import com.piotrcapecki.bakelivery.messaging.model.Thread;
import com.piotrcapecki.bakelivery.messaging.model.ThreadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ThreadRepositoryTest {

    @Autowired
    private ThreadRepository threadRepository;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private Thread buildThread(UUID bakeryId, UUID orderId, UUID customerId) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .orderId(orderId)
                .customerId(customerId)
                .status(ThreadStatus.OPEN)
                .build();
    }

    @Test
    void findByBakeryIdAndOrderId_returnsThread() {
        UUID bakeryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        threadRepository.save(buildThread(bakeryId, orderId, customerId));

        Optional<Thread> found = threadRepository.findByBakeryIdAndOrderId(bakeryId, orderId);

        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void findByBakeryIdAndCustomerId_returnsAllCustomerThreads() {
        UUID bakeryId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        threadRepository.save(buildThread(bakeryId, UUID.randomUUID(), customerId));
        threadRepository.save(buildThread(bakeryId, UUID.randomUUID(), customerId));
        threadRepository.save(buildThread(bakeryId, UUID.randomUUID(), UUID.randomUUID()));

        List<Thread> threads = threadRepository.findByBakeryIdAndCustomerId(bakeryId, customerId);

        assertThat(threads).hasSize(2);
    }

    @Test
    void existsByBakeryIdAndOrderId_returnsTrueWhenExists() {
        UUID bakeryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        threadRepository.save(buildThread(bakeryId, orderId, UUID.randomUUID()));

        assertThat(threadRepository.existsByBakeryIdAndOrderId(bakeryId, orderId)).isTrue();
    }

    @Test
    void findByBakeryIdAndDriverId_returnsAssignedThreads() {
        UUID bakeryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Thread thread = buildThread(bakeryId, UUID.randomUUID(), UUID.randomUUID());
        thread.setDriverId(driverId);
        threadRepository.save(thread);
        threadRepository.save(buildThread(bakeryId, UUID.randomUUID(), UUID.randomUUID()));

        List<Thread> threads = threadRepository.findByBakeryIdAndDriverId(bakeryId, driverId);

        assertThat(threads).hasSize(1);
        assertThat(threads.get(0).getDriverId()).isEqualTo(driverId);
    }
}
