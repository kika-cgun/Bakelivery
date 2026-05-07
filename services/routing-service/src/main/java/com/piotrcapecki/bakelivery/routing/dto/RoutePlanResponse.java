package com.piotrcapecki.bakelivery.routing.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RoutePlanResponse(UUID id, UUID driverId, LocalDate date, String status,
                                 Double totalDistanceMeters, Double totalDurationSeconds) {}
