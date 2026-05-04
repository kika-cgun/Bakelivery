package com.piotrcapecki.bakelivery.customer.security;

import java.util.UUID;

public record CustomerPrincipal(UUID userId, String email, UUID bakeryId, String role) {}
