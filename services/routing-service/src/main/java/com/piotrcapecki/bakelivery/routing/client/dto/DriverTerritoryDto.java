package com.piotrcapecki.bakelivery.routing.client.dto;

import java.util.UUID;

public record DriverTerritoryDto(UUID id, UUID driverId, String driverName,
                                  UUID fixedPointId, String fixedPointName, int affinityScore) {}
