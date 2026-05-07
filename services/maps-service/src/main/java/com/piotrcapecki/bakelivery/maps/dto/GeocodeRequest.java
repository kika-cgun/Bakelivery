package com.piotrcapecki.bakelivery.maps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeocodeRequest(
    @NotBlank @Size(min = 3, max = 500) String address
) {}
