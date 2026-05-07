package com.piotrcapecki.bakelivery.routing.client.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DispatchStopDto(
        UUID id, LocalDate date, UUID orderId, UUID fixedPointId,
        String customerName, String deliveryAddress, Double lat, Double lon,
        UUID assignedDriverId, String assignedDriverName, String status, String notes) {}
