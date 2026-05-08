package com.piotrcapecki.bakelivery.driverops.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StopProgressResponse(
        UUID id,
        int sequenceNumber,
        UUID dispatchStopId,
        String customerName,
        String deliveryAddress,
        Double lat,
        Double lon,
        String status,
        String proofUrl,
        Integer etaSeconds,
        LocalDateTime completedAt,
        String skippedReason
) {}
