package com.piotrcapecki.bakelivery.messaging.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String senderRole,
        String content,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}
