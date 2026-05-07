package com.piotrcapecki.bakelivery.order.security;

import java.util.UUID;

public record OrderPrincipal(UUID userId, String email, UUID bakeryId, String role) {}
