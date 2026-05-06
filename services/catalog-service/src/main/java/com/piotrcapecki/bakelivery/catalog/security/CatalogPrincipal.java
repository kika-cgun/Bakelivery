package com.piotrcapecki.bakelivery.catalog.security;

import java.util.UUID;

public record CatalogPrincipal(UUID userId, String email, UUID bakeryId, String role) {}
