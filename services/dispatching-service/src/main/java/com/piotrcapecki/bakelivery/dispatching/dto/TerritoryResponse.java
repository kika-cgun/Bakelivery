package com.piotrcapecki.bakelivery.dispatching.dto;

import com.piotrcapecki.bakelivery.dispatching.model.DriverTerritory;

import java.util.UUID;

public record TerritoryResponse(
        UUID id,
        UUID driverId,
        String driverName,
        UUID fixedPointId,
        String fixedPointName,
        int affinityScore
) {
    public static TerritoryResponse from(DriverTerritory t) {
        return new TerritoryResponse(
                t.getId(), t.getDriverId(), t.getDriverName(),
                t.getFixedPoint().getId(), t.getFixedPoint().getName(),
                t.getAffinityScore());
    }
}
