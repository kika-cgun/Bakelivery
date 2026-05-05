package com.piotrcapecki.bakelivery.catalog.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateVariantRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 60) String sku,
        @NotNull BigDecimal priceDelta,
        Integer sortOrder
) {}
