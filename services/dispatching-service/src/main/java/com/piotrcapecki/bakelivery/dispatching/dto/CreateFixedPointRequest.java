package com.piotrcapecki.bakelivery.dispatching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFixedPointRequest(
        @NotBlank String name,
        @NotBlank String address,
        Double lat,
        Double lon,
        Short deliveryDays,
        String defaultNotes
) {}
