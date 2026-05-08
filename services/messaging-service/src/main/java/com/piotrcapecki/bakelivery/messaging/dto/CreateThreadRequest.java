package com.piotrcapecki.bakelivery.messaging.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateThreadRequest(
        @NotNull UUID orderId,
        @NotNull UUID bakeryId
) {}
