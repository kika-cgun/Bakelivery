package com.piotrcapecki.bakelivery.messaging.repository;

import com.piotrcapecki.bakelivery.messaging.model.Message;
import com.piotrcapecki.bakelivery.messaging.model.SenderRole;
import com.piotrcapecki.bakelivery.messaging.model.Thread;
import com.piotrcapecki.bakelivery.messaging.model.ThreadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageRepositoryTest {

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private MessageRepository messageRepository;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private Thread savedThread;

    @BeforeEach
    void setUp() {
        savedThread = threadRepository.save(Thread.builder()
                .id(UUID.randomUUID())
                .bakeryId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .status(ThreadStatus.OPEN)
                .build());
    }

    @Test
    void findByThreadIdOrderByCreatedAtAsc_returnsPaginatedMessages() {
        UUID bakeryId = savedThread.getBakeryId();
        for (int i = 0; i < 5; i++) {
            messageRepository.save(Message.builder()
                    .id(UUID.randomUUID())
                    .bakeryId(bakeryId)
                    .thread(savedThread)
                    .senderId(UUID.randomUUID())
                    .senderRole(SenderRole.CUSTOMER)
                    .content("Message " + i)
                    .build());
        }

        Page<Message> page = messageRepository.findByThreadIdOrderByCreatedAtAsc(
                savedThread.getId(), PageRequest.of(0, 3));

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
    }
}
