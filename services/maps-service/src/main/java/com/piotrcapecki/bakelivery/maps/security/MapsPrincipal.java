package com.piotrcapecki.bakelivery.maps.security;

import java.util.UUID;

public record MapsPrincipal(UUID userId, String email, UUID bakeryId, String role) {}
