package com.piotrcapecki.bakelivery.auth.service;

import com.piotrcapecki.bakelivery.auth.dto.AuthResponse;
import com.piotrcapecki.bakelivery.auth.dto.LoginRequest;
import com.piotrcapecki.bakelivery.auth.dto.RegisterRequest;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.jwt.JwtClaims;
import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use");
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();
        User saved = userRepository.save(user);
        return buildResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return buildResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        RefreshTokenService.RotateResult rotated = refreshTokenService.verifyAndRotate(refreshToken);
        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + rotated.userId()));
        return new AuthResponse(
                accessToken(user),
                rotated.newRawToken(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public void logout(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                accessToken(user),
                refreshTokenService.issue(user.getId()),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private String accessToken(User user) {
        return jwtUtil.generateAccessToken(new JwtClaims(
                user.getEmail(),
                user.getId(),
                bakeryId(user),
                user.getRole().name()
        ));
    }

    private UUID bakeryId(User user) {
        return user.getBakery() == null ? null : user.getBakery().getId();
    }
}
