package com.piotrcapecki.bakelivery.catalog.dto;

import jakarta.validation.constraints.*;

public record UpdateCategoryRequest(
        @Size(max = 120) String name,
        @Pattern(regexp = "^[a-z0-9-]+$") @Size(max = 120) String slug,
        Integer sortOrder
) {}
