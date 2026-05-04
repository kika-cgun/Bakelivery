package com.piotrcapecki.bakelivery.common.jwt;

import java.util.UUID;

public record JwtClaims(
        String email,
        UUID userId,
        UUID bakeryId,
        String role
) {}
