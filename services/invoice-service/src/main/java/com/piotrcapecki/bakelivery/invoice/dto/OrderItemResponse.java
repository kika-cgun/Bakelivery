package com.piotrcapecki.bakelivery.invoice.dto;

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
) {}
