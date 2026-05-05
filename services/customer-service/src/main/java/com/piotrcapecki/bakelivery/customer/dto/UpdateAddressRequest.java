package com.piotrcapecki.bakelivery.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(max = 100) String label,
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 100) String city,
        Double latitude,
        Double longitude
) {}
