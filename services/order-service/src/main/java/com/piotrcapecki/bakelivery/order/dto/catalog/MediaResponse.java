package com.piotrcapecki.bakelivery.order.dto.catalog;

import java.util.UUID;

public record MediaResponse(
        UUID id,
        String url,
        String contentType,
        long sizeBytes,
        int sortOrder,
        boolean primary
) {}
