package com.piotrcapecki.bakelivery.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantResponse(UUID id, String name, String sku, BigDecimal priceDelta, int sortOrder) {}
