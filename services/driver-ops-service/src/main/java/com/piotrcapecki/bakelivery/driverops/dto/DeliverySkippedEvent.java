package com.piotrcapecki.bakelivery.driverops.dto;

import java.util.UUID;

public record DeliverySkippedEvent(
        UUID dispatchStopId,
        UUID orderId,
        UUID bakeryId,
        UUID driverId,
        String reason
) {}
