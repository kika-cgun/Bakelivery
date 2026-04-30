package com.piotrcapecki.bakelivery.auth.service;

import com.piotrcapecki.bakelivery.auth.dto.AuthResponse;
import com.piotrcapecki.bakelivery.auth.dto.LoginRequest;
import com.piotrcapecki.bakelivery.auth.dto.RegisterRequest;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import com.piotrcapecki.bakelivery.common.jwt.JwtClaims;
import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService authService;

    @Test
    void register_savesUserAndReturnsToken() {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest("user@test.com", "password123");
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPw");
        when(userRepository.save(any(User.class))).thenReturn(User.builder()
                .id(userId)
                .email("user@test.com")
                .passwordHash("hashedPw")
                .role(Role.USER)
                .build());
        when(jwtUtil.generateAccessToken(any(JwtClaims.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("user@test.com");
        verify(userRepository).save(any(User.class));
        ArgumentCaptor<JwtClaims> claimsCaptor = ArgumentCaptor.forClass(JwtClaims.class);
        verify(jwtUtil).generateAccessToken(claimsCaptor.capture());
        assertThat(claimsCaptor.getValue()).isEqualTo(new JwtClaims("user@test.com", userId, null, "USER"));
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("user@test.com", "pass123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("user@test.com")
                .passwordHash("hashedPw")
                .role(Role.USER)
                .build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPw")).thenReturn(true);
        when(jwtUtil.generateAccessToken(argThat(claims ->
                claims.email().equals("user@test.com")
                        && claims.userId().equals(userId)
                        && claims.bakeryId() == null
                        && claims.role().equals("USER")
        ))).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("user@test.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("user@test.com");
    }

    @Test
    void login_throwsForUnknownEmail() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@test.com", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsForWrongPassword() {
        User user = User.builder()
                .email("user@test.com")
                .passwordHash("hashedPw")
                .role(Role.USER)
                .build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashedPw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@test.com", "wrongpass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loadUserByUsername_returnsUserForKnownEmail() {
        User user = User.builder()
                .email("user@test.com")
                .passwordHash("hashedPw")
                .role(Role.USER)
                .build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        UserDetails result = authService.loadUserByUsername("user@test.com");

        assertThat(result.getUsername()).isEqualTo("user@test.com");
    }

    @Test
    void loadUserByUsername_throwsForUnknownEmail() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("unknown@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
