package com.piotrcapecki.bakelivery.customer.dto;

import com.piotrcapecki.bakelivery.customer.model.DeliveryAddress;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        UUID customerId,
        String label,
        String street,
        String postalCode,
        String city,
        Double latitude,
        Double longitude
) {
    public static AddressResponse from(DeliveryAddress a) {
        return new AddressResponse(a.getId(), a.getCustomerId(), a.getLabel(), a.getStreet(),
                a.getPostalCode(), a.getCity(), a.getLatitude(), a.getLongitude());
    }
}
