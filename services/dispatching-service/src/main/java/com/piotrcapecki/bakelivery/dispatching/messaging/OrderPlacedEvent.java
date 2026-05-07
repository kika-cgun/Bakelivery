package com.piotrcapecki.bakelivery.dispatching.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        UUID bakeryId,
        UUID customerId,
        String customerName,
        String deliveryAddress,
        Double lat,
        Double lon,
        BigDecimal totalAmount,
        LocalDateTime placedAt
) {}
