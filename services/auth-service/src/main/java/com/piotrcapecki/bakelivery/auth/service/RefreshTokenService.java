package com.piotrcapecki.bakelivery.auth.service;

import com.piotrcapecki.bakelivery.auth.model.RefreshToken;
import com.piotrcapecki.bakelivery.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final long ttlMillis;

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${jwt.refresh-ttl-millis}") long ttlMillis) {
        this.repo = repo;
        this.ttlMillis = ttlMillis;
    }

    @Transactional
    public String issue(UUID userId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repo.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plusMillis(ttlMillis))
                .revoked(false)
                .build());
        return rawToken;
    }

    public record RotateResult(UUID userId, String newRawToken) {}

    @Transactional
    public RotateResult verifyAndRotate(String rawToken) {
        String tokenHash = hash(rawToken);
        Instant now = Instant.now();
        RefreshToken current = repo.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!current.getExpiresAt().isAfter(now)) {
            throw new BadCredentialsException("Expired refresh token");
        }
        int revokedRows = repo.revokeIfUsable(tokenHash, now);
        if (revokedRows != 1) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        String newRawToken = issue(current.getUserId());
        return new RotateResult(current.getUserId(), newRawToken);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repo.revokeAllForUser(userId);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
