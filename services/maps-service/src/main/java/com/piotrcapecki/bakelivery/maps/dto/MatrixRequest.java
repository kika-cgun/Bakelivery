package com.piotrcapecki.bakelivery.maps.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MatrixRequest(
    @NotEmpty @Size(max = 100) @Valid List<Coordinate> sources,
    @NotEmpty @Size(max = 100) @Valid List<Coordinate> destinations
) {}
