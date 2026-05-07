package com.piotrcapecki.bakelivery.notification.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        UUID bakeryId,
        UUID customerId,
        String customerEmail,
        String deliveryAddress,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        OffsetDateTime placedAt
) {}
