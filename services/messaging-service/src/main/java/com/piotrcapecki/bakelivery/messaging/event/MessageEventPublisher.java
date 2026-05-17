package com.piotrcapecki.bakelivery.messaging.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.messaging.config.RabbitConfig;
import com.piotrcapecki.bakelivery.messaging.dto.MessageCreatedEvent;
import com.piotrcapecki.bakelivery.messaging.model.Message;
import com.piotrcapecki.bakelivery.messaging.model.Thread;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(MessageEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishMessageCreated(Message message, Thread thread) {
        MessageCreatedEvent event = new MessageCreatedEvent(
                thread.getId(),
                message.getId(),
                thread.getBakeryId(),
                message.getSenderId(),
                message.getSenderRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );

        // Publish to RabbitMQ
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_MESSAGE_CREATED, event);
        logger.info("Published message.created event for messageId={}", message.getId());

        // Publish to Redis for realtime push
        try {
            String payload = objectMapper.writeValueAsString(event);
            String channel = "messaging.thread." + thread.getId();
            redisTemplate.convertAndSend(channel, payload);
            logger.debug("Published to Redis channel={}", channel);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize MessageCreatedEvent for Redis", e);
        }
    }
}
