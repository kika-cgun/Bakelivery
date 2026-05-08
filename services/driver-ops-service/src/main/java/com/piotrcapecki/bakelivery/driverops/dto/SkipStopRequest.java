package com.piotrcapecki.bakelivery.driverops.dto;

import jakarta.validation.constraints.NotBlank;

public record SkipStopRequest(@NotBlank String reason) {}
