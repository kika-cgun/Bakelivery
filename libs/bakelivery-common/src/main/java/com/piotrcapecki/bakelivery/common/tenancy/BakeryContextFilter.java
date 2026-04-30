package com.piotrcapecki.bakelivery.common.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class BakeryContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("X-Bakery-Id");
        try {
            if (header != null && !header.isBlank()) {
                try {
                    BakeryContext.set(UUID.fromString(header));
                } catch (IllegalArgumentException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid X-Bakery-Id header\"}");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            BakeryContext.clear();
        }
    }
}
