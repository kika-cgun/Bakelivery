package com.piotrcapecki.bakelivery.catalog.dto;

import java.util.List;

public record ProductDetailResponse(
        ProductResponse product,
        List<VariantResponse> variants,
        List<MediaResponse> media
) {}
