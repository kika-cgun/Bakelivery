package com.piotrcapecki.bakelivery.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBiYWtlbGl2ZXJ5";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    @Test
    void generateToken_returnsNonEmptyToken() {
        String token = jwtUtil.generateToken("user@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("user@test.com");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    void isTokenValid_trueForMatchingEmail() {
        String token = jwtUtil.generateToken("user@test.com");
        assertThat(jwtUtil.isTokenValid(token, "user@test.com")).isTrue();
    }

    @Test
    void isTokenValid_falseForDifferentEmail() {
        String token = jwtUtil.generateToken("user@test.com");
        assertThat(jwtUtil.isTokenValid(token, "other@test.com")).isFalse();
    }

    @Test
    void isTokenValid_falseForTamperedToken() {
        assertThat(jwtUtil.isTokenValid("not.a.token", "user@test.com")).isFalse();
    }
}
