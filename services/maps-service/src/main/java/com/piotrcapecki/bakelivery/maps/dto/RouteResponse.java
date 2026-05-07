package com.piotrcapecki.bakelivery.maps.dto;

import java.util.List;

public record RouteResponse(
    double distance,
    double duration,
    String geometry,
    List<RouteStep> steps
) {
    public record RouteStep(
        double distance,
        double duration,
        String instruction,
        String name
    ) {}
}
