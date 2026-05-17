package com.piotrcapecki.bakelivery.messaging.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ThreadResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        UUID driverId,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
