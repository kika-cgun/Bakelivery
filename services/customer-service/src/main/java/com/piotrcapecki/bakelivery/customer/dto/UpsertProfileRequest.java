package com.piotrcapecki.bakelivery.customer.dto;

import com.piotrcapecki.bakelivery.customer.model.CustomerType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertProfileRequest(
        @NotNull CustomerType type,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 40) String phone,
        @Size(max = 200) String companyName,
        @Size(max = 50) String vatId,
        @Size(max = 500) String billingAddress) {

    public UpsertProfileRequest {
        if (type == CustomerType.COMPANY) {
            if (companyName == null || companyName.isBlank()) {
                throw new IllegalArgumentException("companyName required for COMPANY type");
            }
            if (vatId == null || vatId.isBlank()) {
                throw new IllegalArgumentException("vatId required for COMPANY type");
            }
        }
    }
}
