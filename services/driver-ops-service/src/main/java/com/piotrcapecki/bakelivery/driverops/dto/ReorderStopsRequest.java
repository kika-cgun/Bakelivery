package com.piotrcapecki.bakelivery.driverops.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderStopsRequest(@NotEmpty List<UUID> stopIds) {}
