package com.piotrcapecki.bakelivery.customer.config;

import com.piotrcapecki.bakelivery.common.jwt.JwtClaims;
import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import com.piotrcapecki.bakelivery.customer.security.CustomerPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            JwtClaims claims = jwtUtil.parse(authHeader.substring(7));
            CustomerPrincipal principal = new CustomerPrincipal(
                    claims.userId(), claims.email(), claims.bakeryId(), claims.role());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
