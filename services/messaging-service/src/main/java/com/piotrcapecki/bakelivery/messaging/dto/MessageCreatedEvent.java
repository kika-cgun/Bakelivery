package com.piotrcapecki.bakelivery.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageCreatedEvent(
        UUID threadId,
        UUID messageId,
        UUID bakeryId,
        UUID senderId,
        String senderRole,
        String content,
        LocalDateTime createdAt
) {}
