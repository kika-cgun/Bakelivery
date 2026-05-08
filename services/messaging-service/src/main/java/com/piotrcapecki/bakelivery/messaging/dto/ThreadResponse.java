package com.piotrcapecki.bakelivery.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ThreadResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        UUID driverId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
