package com.piotrcapecki.bakelivery.dispatching.dto;

import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;

import java.util.UUID;

public record FixedPointResponse(
        UUID id,
        String name,
        String address,
        Double lat,
        Double lon,
        short deliveryDays,
        String defaultNotes,
        boolean active
) {
    public static FixedPointResponse from(FixedDeliveryPoint p) {
        return new FixedPointResponse(
                p.getId(), p.getName(), p.getAddress(),
                p.getLat(), p.getLon(), p.getDeliveryDays(),
                p.getDefaultNotes(), p.isActive());
    }
}
