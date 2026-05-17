package com.piotrcapecki.bakelivery.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryCompletedEvent(
        UUID dispatchStopId,
        UUID orderId,
        UUID bakeryId,
        UUID driverId,
        UUID shiftId,
        boolean hasProof,
        LocalDateTime completedAt
) {}
