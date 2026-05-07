package com.piotrcapecki.bakelivery.order.dto;

import com.piotrcapecki.bakelivery.order.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID bakeryId,
        UUID customerId,
        String status,
        BigDecimal totalAmount,
        String deliveryAddress,
        String notes,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public static OrderResponse of(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBakeryId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getNotes(),
                order.getItems().stream().map(OrderItemResponse::of).toList(),
                order.getCreatedAt() == null ? null : order.getCreatedAt().toLocalDateTime());
    }
}
