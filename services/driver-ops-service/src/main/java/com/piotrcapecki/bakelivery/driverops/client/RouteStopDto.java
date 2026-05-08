package com.piotrcapecki.bakelivery.driverops.client;

import java.util.UUID;

public record RouteStopDto(
        UUID id,
        UUID dispatchStopId,
        int sequenceNumber,
        double lat,
        double lon,
        String customerName,
        String deliveryAddress,
        double affinityScore,
        Integer etaSeconds,
        String status
) {}
