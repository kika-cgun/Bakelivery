package com.piotrcapecki.bakelivery.gateway.filter;

import com.piotrcapecki.bakelivery.common.jwt.JwtClaims;
import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

public class JwtPropagationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final List<String> PUBLIC_EXACT_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    );

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/v3/api-docs",
            "/swagger-ui"
    );

    private final JwtUtil jwtUtil;

    public JwtPropagationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        String path = request.path();

        if (isPublicPath(path)) {
            return next.handle(request);
        }

        String authHeader = request.headers().firstHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ServerResponse.status(401)
                    .header("WWW-Authenticate", "Bearer realm=\"bakelivery\"")
                    .build();
        }

        String token = authHeader.substring(7);
        JwtClaims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return ServerResponse.status(401)
                    .header("WWW-Authenticate", "Bearer realm=\"bakelivery\"")
                    .build();
        }

        ServerRequest mutated = ServerRequest.from(request)
                .header("X-User-Id", claims.userId().toString())
                .header("X-Role", claims.role())
                .headers(headers -> {
                    if (claims.bakeryId() != null) {
                        headers.set("X-Bakery-Id", claims.bakeryId().toString());
                    }
                })
                .build();

        return next.handle(mutated);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_EXACT_PATHS.contains(path)
                || PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
