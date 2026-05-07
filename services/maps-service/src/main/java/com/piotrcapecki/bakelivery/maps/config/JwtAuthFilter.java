package com.piotrcapecki.bakelivery.maps.config;

import com.piotrcapecki.bakelivery.maps.security.MapsPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userIdStr = request.getHeader("X-User-Id");
        String role = request.getHeader("X-Role");
        String bakeryIdStr = request.getHeader("X-Bakery-Id");

        if (userIdStr != null && role != null) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                UUID bakeryId = bakeryIdStr != null ? UUID.fromString(bakeryIdStr) : null;
                MapsPrincipal principal = new MapsPrincipal(userId, null, bakeryId, role);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException ignored) {
                // Malformed UUID header — proceed unauthenticated, security chain will reject
            }
        }
        filterChain.doFilter(request, response);
    }
}
