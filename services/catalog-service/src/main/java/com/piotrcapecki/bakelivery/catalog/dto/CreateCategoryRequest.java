package com.piotrcapecki.bakelivery.catalog.dto;

import jakarta.validation.constraints.*;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
        Integer sortOrder
) {}
