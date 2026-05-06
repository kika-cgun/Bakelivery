package com.piotrcapecki.bakelivery.catalog.dto;

import com.piotrcapecki.bakelivery.catalog.model.ProductVariant;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantResponse(UUID id, String name, String sku, BigDecimal priceDelta, int sortOrder) {
    public static VariantResponse of(ProductVariant v) {
        return new VariantResponse(v.getId(), v.getName(), v.getSku(), v.getPriceDelta(), v.getSortOrder());
    }
}
