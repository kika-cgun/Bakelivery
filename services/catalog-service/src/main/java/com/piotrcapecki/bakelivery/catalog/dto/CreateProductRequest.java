package com.piotrcapecki.bakelivery.catalog.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        UUID categoryId,
        @Size(max = 60) String sku,
        @NotBlank @Size(max = 160) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal basePrice,
        @Min(0) @Max(127) Short availableDays
) {}
