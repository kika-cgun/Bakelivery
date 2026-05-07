package com.piotrcapecki.bakelivery.dispatching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTerritoryRequest(
        @NotNull UUID driverId,
        @NotBlank String driverName,
        @NotNull UUID fixedPointId
) {}
