package com.piotrcapecki.bakelivery.dispatching.dto;

import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;

import java.time.LocalDate;
import java.util.UUID;

public record DispatchStopResponse(
        UUID id,
        LocalDate date,
        UUID orderId,
        UUID fixedPointId,
        String customerName,
        String deliveryAddress,
        Double lat,
        Double lon,
        UUID assignedDriverId,
        String assignedDriverName,
        String status,
        String notes
) {
    public static DispatchStopResponse from(DispatchStop s) {
        return new DispatchStopResponse(
                s.getId(), s.getDate(), s.getOrderId(), s.getFixedPointId(),
                s.getCustomerName(), s.getDeliveryAddress(), s.getLat(), s.getLon(),
                s.getAssignedDriverId(), s.getAssignedDriverName(),
                s.getStatus().name(), s.getNotes());
    }
}
