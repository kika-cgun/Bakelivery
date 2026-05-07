package com.piotrcapecki.bakelivery.order.dto;

import com.piotrcapecki.bakelivery.order.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID variantId,
        String variantName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
    public static OrderItemResponse of(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getVariantId(),
                item.getVariantName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal());
    }
}
