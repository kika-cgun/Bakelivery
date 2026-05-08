package com.piotrcapecki.bakelivery.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String senderRole,
        String content,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}
