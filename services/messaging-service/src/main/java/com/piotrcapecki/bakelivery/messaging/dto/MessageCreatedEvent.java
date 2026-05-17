package com.piotrcapecki.bakelivery.messaging.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageCreatedEvent(
        UUID threadId,
        UUID messageId,
        UUID bakeryId,
        UUID senderId,
        String senderRole,
        String content,
        OffsetDateTime createdAt
) {}
