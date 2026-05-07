package com.piotrcapecki.bakelivery.dispatching.dto;

public record UpdateFixedPointRequest(
        String name,
        String address,
        Double lat,
        Double lon,
        Short deliveryDays,
        String defaultNotes
) {}
