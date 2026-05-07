package com.piotrcapecki.bakelivery.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateOrderStatusRequest(
        @NotBlank
        @Pattern(regexp = "PLACED|ACCEPTED|READY|IN_DELIVERY|DELIVERED|CANCELLED",
                 message = "Invalid order status")
        String status) {}
