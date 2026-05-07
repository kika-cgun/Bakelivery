package com.piotrcapecki.bakelivery.dispatching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDriverRequest(
        @NotNull UUID driverId,
        @NotBlank String driverName
) {}
