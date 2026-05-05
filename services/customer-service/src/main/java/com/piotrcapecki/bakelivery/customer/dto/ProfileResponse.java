package com.piotrcapecki.bakelivery.customer.dto;

import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.model.CustomerType;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        UUID bakeryId,
        CustomerType type,
        String firstName,
        String lastName,
        String phone,
        String companyName,
        String vatId,
        String billingAddress
) {
    public static ProfileResponse from(Customer c) {
        return new ProfileResponse(
                c.getId(), c.getUserId(), c.getBakeryId(), c.getType(),
                c.getFirstName(), c.getLastName(), c.getPhone(),
                c.getCompanyName(), c.getVatId(), c.getBillingAddress());
    }
}
