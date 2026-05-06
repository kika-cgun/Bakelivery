package com.piotrcapecki.bakelivery.catalog.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateVariantRequest(
        @Size(max = 80) String name,
        @Size(max = 60) String sku,
        BigDecimal priceDelta,
        Integer sortOrder
) {}
