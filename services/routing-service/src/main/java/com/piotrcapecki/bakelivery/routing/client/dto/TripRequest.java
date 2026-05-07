package com.piotrcapecki.bakelivery.routing.client.dto;

import java.util.List;

public record TripRequest(List<CoordinateDto> waypoints) {}
