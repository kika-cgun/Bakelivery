package com.piotrcapecki.bakelivery.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {

    private final SecretKey key;
    private final long accessTtlMillis;

    public JwtUtil(String base64Secret, long accessTtlMillis) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTtlMillis = accessTtlMillis;
    }

    public String generateAccessToken(JwtClaims claims) {
        var builder = Jwts.builder()
                .subject(claims.email())
                .claim("userId", claims.userId().toString())
                .claim("role", claims.role())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTtlMillis));
        if (claims.bakeryId() != null) {
            builder.claim("bakeryId", claims.bakeryId().toString());
        }
        return builder.signWith(key).compact();
    }

    public JwtClaims parse(String token) {
        Claims c = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String subject = c.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("JWT subject is required");
        }
        String userIdStr = c.get("userId", String.class);
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new JwtException("JWT userId is required");
        }
        String role = c.get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new JwtException("JWT role is required");
        }
        String bakeryIdStr = c.get("bakeryId", String.class);
        return new JwtClaims(
                subject,
                parseUuidClaim("userId", userIdStr),
                bakeryIdStr == null ? null : parseUuidClaim("bakeryId", bakeryIdStr),
                role
        );
    }

    private UUID parseUuidClaim(String claimName, String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new JwtException("JWT " + claimName + " must be a valid UUID", e);
        }
    }
}
