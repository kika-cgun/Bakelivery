package com.piotrcapecki.bakelivery.driverops.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        UUID driverId,
        LocalDate date,
        UUID routePlanId,
        String status,
        int currentStopIndex,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {}
