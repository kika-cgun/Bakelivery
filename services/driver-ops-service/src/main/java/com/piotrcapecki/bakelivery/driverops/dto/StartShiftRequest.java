package com.piotrcapecki.bakelivery.driverops.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartShiftRequest(@NotNull UUID routePlanId) {}
