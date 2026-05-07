package com.piotrcapecki.bakelivery.routing.event;

import java.time.LocalDate;
import java.util.UUID;

public record RouteUpdatedEvent(UUID routePlanId, UUID driverId, UUID bakeryId, LocalDate date, int stopCount) {}
