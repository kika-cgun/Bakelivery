package com.piotrcapecki.bakelivery.order.dto.catalog;

import java.util.List;

public record ProductDetailResponse(
        ProductResponse product,
        List<VariantResponse> variants,
        List<MediaResponse> media
) {}
