package com.piotrcapecki.bakelivery.common.jwt;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBiYWtlbGl2ZXJ5";
    private JwtUtil jwt;

    @BeforeEach
    void setUp() {
        jwt = new JwtUtil(SECRET, 3600_000L);
    }

    @Test
    void generate_andParse_roundTrip() {
        UUID userId = UUID.randomUUID();
        UUID bakeryId = UUID.randomUUID();
        String token = jwt.generateAccessToken(new JwtClaims("a@b.com", userId, bakeryId, "DISPATCHER"));

        JwtClaims parsed = jwt.parse(token);
        assertThat(parsed.email()).isEqualTo("a@b.com");
        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.bakeryId()).isEqualTo(bakeryId);
        assertThat(parsed.role()).isEqualTo("DISPATCHER");
    }

    @Test
    void parse_handlesNullBakeryId_forSuperAdmin() {
        UUID userId = UUID.randomUUID();
        String token = jwt.generateAccessToken(new JwtClaims("super@bake.com", userId, null, "SUPER_ADMIN"));
        JwtClaims parsed = jwt.parse(token);
        assertThat(parsed.bakeryId()).isNull();
        assertThat(parsed.role()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void parse_throwsForTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwt.generateAccessToken(new JwtClaims("a@b.com", userId, null, "SUPER_ADMIN"));
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        org.junit.jupiter.api.Assertions.assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwt.parse(tampered)
        );
    }

    @Test
    void parse_throwsJwtExceptionForMissingSubject() {
        String token = tokenWithClaims(null, UUID.randomUUID().toString(), "SUPER_ADMIN", null);

        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtExceptionForBlankSubject() {
        String token = tokenWithClaims("   ", UUID.randomUUID().toString(), "SUPER_ADMIN", null);

        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtExceptionForMissingUserId() {
        String token = tokenWithClaims("a@b.com", null, "SUPER_ADMIN", null);

        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtExceptionForMalformedUserId() {
        String token = tokenWithClaims("a@b.com", "not-a-uuid", "SUPER_ADMIN", null);

        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtExceptionForMissingRole() {
        String token = tokenWithClaims("a@b.com", UUID.randomUUID().toString(), null, null);

        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtExceptionForBlankRole() {
        String token = tokenWithClaims("a@b.com", UUID.randomUUID().toString(), "   ", null);

        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_throwsJwtExceptionForMalformedBakeryId() {
        String token = tokenWithClaims("a@b.com", UUID.randomUUID().toString(), "DISPATCHER", "not-a-uuid");

        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(JwtException.class);
    }

    private String tokenWithClaims(String subject, String userId, String role, String bakeryId) {
        return tokenWith(builder -> {
            if (subject != null) {
                builder.subject(subject);
            }
            if (userId != null) {
                builder.claim("userId", userId);
            }
            if (role != null) {
                builder.claim("role", role);
            }
            if (bakeryId != null) {
                builder.claim("bakeryId", bakeryId);
            }
        });
    }

    private String tokenWith(Consumer<JwtBuilder> customizer) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        JwtBuilder builder = Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000L));
        customizer.accept(builder);
        return builder.signWith(key).compact();
    }
}
