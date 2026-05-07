package com.piotrcapecki.bakelivery.routing.client.dto;

import java.util.List;

public record TripResponse(String geoJsonGeometry, double totalDistanceMeters,
                            double totalDurationSeconds, List<LegDto> legs,
                            List<Integer> waypointOrder) {}
