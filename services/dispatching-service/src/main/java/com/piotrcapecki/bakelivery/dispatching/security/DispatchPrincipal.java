package com.piotrcapecki.bakelivery.dispatching.security;

import java.util.UUID;

public record DispatchPrincipal(UUID userId, String email, UUID bakeryId, String role) {}
