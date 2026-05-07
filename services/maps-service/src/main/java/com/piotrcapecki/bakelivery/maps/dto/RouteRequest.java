package com.piotrcapecki.bakelivery.maps.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RouteRequest(
    @NotEmpty @Size(min = 2, max = 50) @Valid List<Coordinate> waypoints,
    boolean steps,
    boolean overview
) {}
