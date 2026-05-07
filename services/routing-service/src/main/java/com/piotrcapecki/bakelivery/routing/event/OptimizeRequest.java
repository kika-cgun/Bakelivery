package com.piotrcapecki.bakelivery.routing.event;

import java.time.LocalDate;
import java.util.UUID;

public record OptimizeRequest(UUID bakeryId, LocalDate date, String requestedBy) {}
