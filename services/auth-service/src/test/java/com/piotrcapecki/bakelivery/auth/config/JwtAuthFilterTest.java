package com.piotrcapecki.bakelivery.auth.config;

import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.service.AuthService;
import com.piotrcapecki.bakelivery.common.jwt.JwtClaims;
import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private AuthService authService;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthFilter(jwtUtil, authService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_authenticatesWhenTokenClaimsMatchLoadedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtClaims claims = new JwtClaims("user@test.com", userId, null, "USER");
        User user = User.builder()
                .id(userId)
                .email("user@test.com")
                .passwordHash("hashedPw")
                .role(Role.USER)
                .build();
        when(jwtUtil.parse("valid-token")).thenReturn(claims);
        when(authService.loadUserByUsername("user@test.com")).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@test.com");
        assertThat(chain.getRequest()).isSameAs(request);
        verify(jwtUtil).parse("valid-token");
    }

    @Test
    void doFilter_doesNotAuthenticateInvalidJwtAndContinuesChain() throws Exception {
        when(jwtUtil.parse("invalid-token")).thenThrow(new JwtException("Invalid token"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(authService);
    }

    @Test
    void doFilter_doesNotAuthenticateWhenLoadedUsernameDiffersFromClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtClaims claims = new JwtClaims("user@test.com", userId, null, "USER");
        UserDetails user = org.springframework.security.core.userdetails.User
                .withUsername("other@test.com")
                .password("hashedPw")
                .roles("USER")
                .build();
        when(jwtUtil.parse("valid-token")).thenReturn(claims);
        when(authService.loadUserByUsername("user@test.com")).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verify(authService).loadUserByUsername("user@test.com");
        verify(jwtUtil).parse("valid-token");
    }
}
