package com.piotrcapecki.bakelivery.order.dto.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID categoryId,
        String sku,
        String slug,
        String name,
        String description,
        BigDecimal basePrice,
        short availableDays,
        boolean active
) {}
