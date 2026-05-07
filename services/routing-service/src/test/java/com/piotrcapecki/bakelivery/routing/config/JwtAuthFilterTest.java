package com.piotrcapecki.bakelivery.routing.config;

import com.piotrcapecki.bakelivery.common.jwt.JwtClaims;
import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import com.piotrcapecki.bakelivery.routing.security.RoutingPrincipal;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthFilter filter;

    @BeforeEach
    void setup() {
        jwtUtil = mock(JwtUtil.class);
        filter = new JwtAuthFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @Test
    void filter_validToken_setsRoutingPrincipalAuthentication() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bakeryId = UUID.randomUUID();
        when(jwtUtil.parse("valid-token")).thenReturn(new JwtClaims("driver@x.pl", userId, bakeryId, "DRIVER"));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(RoutingPrincipal.class);
        RoutingPrincipal p = (RoutingPrincipal) auth.getPrincipal();
        assertThat(p.userId()).isEqualTo(userId);
        assertThat(p.bakeryId()).isEqualTo(bakeryId);
        assertThat(p.email()).isEqualTo("driver@x.pl");
        assertThat(p.role()).isEqualTo("DRIVER");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_DRIVER");
        verify(chain).doFilter(req, res);
    }

    @Test
    void filter_missingHeader_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    void filter_invalidToken_doesNotSetAuthenticationButContinuesChain() throws Exception {
        when(jwtUtil.parse(any())).thenThrow(new RuntimeException("invalid"));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer broken");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }
}
