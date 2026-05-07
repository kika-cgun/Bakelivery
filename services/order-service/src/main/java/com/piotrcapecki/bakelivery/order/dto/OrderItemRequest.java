package com.piotrcapecki.bakelivery.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID productId,
        UUID variantId,
        @Min(1) int quantity
) {}
