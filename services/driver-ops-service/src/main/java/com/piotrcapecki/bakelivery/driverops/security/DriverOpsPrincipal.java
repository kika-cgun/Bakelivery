package com.piotrcapecki.bakelivery.driverops.security;

import java.util.UUID;

public record DriverOpsPrincipal(UUID userId, String email, UUID bakeryId, String role) {}
