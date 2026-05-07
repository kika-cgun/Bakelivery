package com.piotrcapecki.bakelivery.routing.dto;

import java.util.UUID;

public record RouteStopResponse(UUID id, UUID dispatchStopId, int sequenceNumber,
                                 double lat, double lon, String customerName, String deliveryAddress,
                                 double affinityScore, Integer etaSeconds, String status) {}
