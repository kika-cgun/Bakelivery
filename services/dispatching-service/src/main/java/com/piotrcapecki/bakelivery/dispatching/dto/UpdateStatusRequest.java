package com.piotrcapecki.bakelivery.dispatching.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStatusRequest(@NotBlank String status) {}
