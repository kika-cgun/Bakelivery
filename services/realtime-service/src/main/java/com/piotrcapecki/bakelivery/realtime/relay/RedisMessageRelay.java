package com.piotrcapecki.bakelivery.realtime.relay;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class RedisMessageRelay implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageRelay.class);

    private final SimpMessagingTemplate messaging;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        if (channel.startsWith("driver.pos.")) {
            String driverId = channel.substring("driver.pos.".length());
            String destination = "/topic/driver.position." + driverId;
            messaging.convertAndSend(destination, body);
            logger.debug("Relayed driver position for driverId={} to {}", driverId, destination);
        } else if (channel.startsWith("messaging.thread.")) {
            String threadId = channel.substring("messaging.thread.".length());
            String destination = "/topic/thread." + threadId;
            messaging.convertAndSend(destination, body);
            logger.debug("Relayed message event for threadId={} to {}", threadId, destination);
        } else {
            logger.warn("Received message on unknown Redis channel: {}", channel);
        }
    }
}
