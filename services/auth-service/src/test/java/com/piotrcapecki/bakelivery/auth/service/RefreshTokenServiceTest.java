package com.piotrcapecki.bakelivery.auth.service;

import com.piotrcapecki.bakelivery.auth.model.RefreshToken;
import com.piotrcapecki.bakelivery.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void issuePersistsHashedTokenAndReturnsRawToken() {
        UUID userId = UUID.randomUUID();
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, 60_000);
        Instant before = Instant.now();

        String rawToken = service.issue(userId);

        Instant after = Instant.now();
        assertThat(rawToken).isNotBlank();
        assertThat(rawToken).doesNotContain("=");
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken saved = tokenCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTokenHash()).isEqualTo(hash(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getExpiresAt())
                .isAfterOrEqualTo(before.plusMillis(60_000))
                .isBeforeOrEqualTo(after.plusMillis(60_000));
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    void verifyAndRotateRejectsUnknownToken() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, 60_000);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hash("unknown-token")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAndRotate("unknown-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void verifyAndRotateRejectsExpiredToken() {
        String rawToken = "expired-token";
        RefreshToken expired = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().minusSeconds(1))
                .revoked(false)
                .build();
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, 60_000);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken)))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.verifyAndRotate(rawToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expired refresh token");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void verifyAndRotateRevokesOldTokenAndIssuesNewToken() {
        UUID userId = UUID.randomUUID();
        String rawToken = "valid-token";
        RefreshToken current = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plusSeconds(60))
                .revoked(false)
                .build();
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, 60_000);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken)))
                .thenReturn(Optional.of(current));

        RefreshTokenService.RotateResult result = service.verifyAndRotate(rawToken);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.newRawToken()).isNotBlank();
        assertThat(result.newRawToken()).isNotEqualTo(rawToken);
        assertThat(current.isRevoked()).isTrue();
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(current);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(tokenCaptor.capture());
        List<RefreshToken> savedTokens = tokenCaptor.getAllValues();
        RefreshToken replacement = savedTokens.get(1);
        assertThat(replacement.getUserId()).isEqualTo(userId);
        assertThat(replacement.getTokenHash()).isEqualTo(hash(result.newRawToken()));
        assertThat(replacement.isRevoked()).isFalse();
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
