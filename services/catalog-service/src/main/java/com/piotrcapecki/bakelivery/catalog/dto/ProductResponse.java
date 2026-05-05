package com.piotrcapecki.bakelivery.catalog.dto;

import com.piotrcapecki.bakelivery.catalog.model.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id, UUID categoryId, String sku, String slug, String name,
        String description, BigDecimal basePrice, short availableDays, boolean active
) {
    public static ProductResponse of(Product p) {
        return new ProductResponse(
                p.getId(), p.getCategoryId(), p.getSku(), p.getSlug(), p.getName(),
                p.getDescription(), p.getBasePrice(), p.getAvailableDays(), p.isActive());
    }
}
