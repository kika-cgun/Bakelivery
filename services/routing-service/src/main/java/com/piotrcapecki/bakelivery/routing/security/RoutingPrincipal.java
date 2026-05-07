package com.piotrcapecki.bakelivery.routing.security;

import java.util.UUID;

public record RoutingPrincipal(UUID userId, String email, UUID bakeryId, String role) {}
