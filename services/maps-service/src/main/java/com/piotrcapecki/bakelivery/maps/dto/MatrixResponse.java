package com.piotrcapecki.bakelivery.maps.dto;

public record MatrixResponse(
    double[][] durations,
    double[][] distances,
    boolean cached
) {}
