package com.piotrcapecki.bakelivery.messaging.security;

import java.util.UUID;

public record MessagingPrincipal(UUID userId, String email, UUID bakeryId, String role) {}
