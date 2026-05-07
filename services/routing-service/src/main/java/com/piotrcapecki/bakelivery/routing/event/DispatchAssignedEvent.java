package com.piotrcapecki.bakelivery.routing.event;

import java.time.LocalDate;
import java.util.UUID;

public record DispatchAssignedEvent(
        UUID dispatchStopId, UUID bakeryId, UUID driverId, String driverName,
        LocalDate date, String deliveryAddress, Double lat, Double lon, UUID orderId) {}
