package com.piotrcapecki.bakelivery.maps.dto;

public record GeocodeResponse(
    double lat,
    double lon,
    String displayName,
    boolean cached
) {}
