package com.piotrcapecki.bakelivery.service;

import com.piotrcapecki.bakelivery.config.JwtUtil;
import com.piotrcapecki.bakelivery.dto.AuthResponse;
import com.piotrcapecki.bakelivery.dto.LoginRequest;
import com.piotrcapecki.bakelivery.dto.RegisterRequest;
import com.piotrcapecki.bakelivery.model.Role;
import com.piotrcapecki.bakelivery.model.User;
import com.piotrcapecki.bakelivery.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        RegisterRequest request = new RegisterRequest("user@test.com", "password123");
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPw");
        when(jwtUtil.generateToken("user@test.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("user@test.com");
        verify(userRepository).save(any(User.class));
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
        User user = User.builder()
                .email("user@test.com")
                .passwordHash("hashedPw")
                .role(Role.USER)
                .build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPw")).thenReturn(true);
        when(jwtUtil.generateToken("user@test.com")).thenReturn("jwt-token");

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
}
