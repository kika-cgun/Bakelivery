package com.piotrcapecki.bakelivery.gateway.filter;

import com.piotrcapecki.bakelivery.common.jwt.JwtClaims;
import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtPropagationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HandlerFunction<ServerResponse> next;

    @Mock
    private ServerResponse okResponse;

    private JwtPropagationFilter filter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BAKERY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        filter = new JwtPropagationFilter(jwtUtil);
    }

    // --- helpers ---

    private ServerRequest buildRequest(String path, String authHeader) {
        MockHttpServletRequest mock = new MockHttpServletRequest("GET", path);
        mock.setServletPath(path);
        if (authHeader != null) {
            mock.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return ServerRequest.create(mock, List.of());
    }

    // --- tests ---

    @Test
    void filter_publicLoginPath_passesWithoutToken() throws Exception {
        ServerRequest request = buildRequest("/api/auth/login", null);
        when(next.handle(any())).thenReturn(okResponse);

        ServerResponse result = filter.filter(request, next);

        assertThat(result).isSameAs(okResponse);
        verify(jwtUtil, never()).parse(any());
    }

    @Test
    void filter_publicRegisterPath_passesWithoutToken() throws Exception {
        ServerRequest request = buildRequest("/api/auth/register", null);
        when(next.handle(any())).thenReturn(okResponse);

        ServerResponse result = filter.filter(request, next);

        assertThat(result).isSameAs(okResponse);
        verify(jwtUtil, never()).parse(any());
    }

    @Test
    void filter_publicRefreshPath_passesWithoutToken() throws Exception {
        ServerRequest request = buildRequest("/api/auth/refresh", null);
        when(next.handle(any())).thenReturn(okResponse);

        ServerResponse result = filter.filter(request, next);

        assertThat(result).isSameAs(okResponse);
        verify(jwtUtil, never()).parse(any());
    }

    @Test
    void filter_swaggerPath_passesWithoutToken() throws Exception {
        ServerRequest request = buildRequest("/swagger-ui/index.html", null);
        when(next.handle(any())).thenReturn(okResponse);

        ServerResponse result = filter.filter(request, next);

        assertThat(result).isSameAs(okResponse);
        verify(jwtUtil, never()).parse(any());
    }

    @Test
    void filter_apiDocsPath_passesWithoutToken() throws Exception {
        ServerRequest request = buildRequest("/v3/api-docs/swagger-config", null);
        when(next.handle(any())).thenReturn(okResponse);

        ServerResponse result = filter.filter(request, next);

        assertThat(result).isSameAs(okResponse);
        verify(jwtUtil, never()).parse(any());
    }

    @Test
    void filter_protectedPath_missingAuthHeader_returns401() throws Exception {
        ServerRequest request = buildRequest("/api/orders/123", null);

        ServerResponse result = filter.filter(request, next);

        assertThat(result.statusCode().value()).isEqualTo(401);
        verify(next, never()).handle(any());
        verify(jwtUtil, never()).parse(any());
    }

    @Test
    void filter_protectedPath_malformedAuthHeader_returns401() throws Exception {
        ServerRequest request = buildRequest("/api/orders/123", "Basic dXNlcjpwYXNz");

        ServerResponse result = filter.filter(request, next);

        assertThat(result.statusCode().value()).isEqualTo(401);
        verify(next, never()).handle(any());
        verify(jwtUtil, never()).parse(any());
    }

    @Test
    void filter_protectedPath_invalidToken_returns401() throws Exception {
        ServerRequest request = buildRequest("/api/orders/123", "Bearer bad-token");
        when(jwtUtil.parse("bad-token")).thenThrow(new JwtException("invalid"));

        ServerResponse result = filter.filter(request, next);

        assertThat(result.statusCode().value()).isEqualTo(401);
        verify(next, never()).handle(any());
    }

    @Test
    void filter_protectedPath_validToken_propagatesXUserIdAndXRole() throws Exception {
        JwtClaims claims = new JwtClaims("user@test.com", USER_ID, BAKERY_ID, "BAKERY_OWNER");
        ServerRequest request = buildRequest("/api/orders/123", "Bearer valid-token");
        when(jwtUtil.parse("valid-token")).thenReturn(claims);
        when(next.handle(any())).thenAnswer(inv -> {
            ServerRequest forwarded = inv.getArgument(0);
            assertThat(forwarded.headers().firstHeader("X-User-Id")).isEqualTo(USER_ID.toString());
            assertThat(forwarded.headers().firstHeader("X-Role")).isEqualTo("BAKERY_OWNER");
            assertThat(forwarded.headers().firstHeader("X-Bakery-Id")).isEqualTo(BAKERY_ID.toString());
            return okResponse;
        });

        ServerResponse result = filter.filter(request, next);

        assertThat(result).isSameAs(okResponse);
    }

    @Test
    void filter_pathWithPublicPrefixButNotExactMatch_returns401() throws Exception {
        // /api/auth/loginFoo should NOT bypass auth — only the exact path /api/auth/login is public
        ServerRequest request = buildRequest("/api/auth/loginFoo", null);

        ServerResponse result = filter.filter(request, next);

        assertThat(result.statusCode().value()).isEqualTo(401);
        verify(next, never()).handle(any());
        verify(jwtUtil, never()).parse(any());
    }

    @Test
    void filter_protectedPath_validTokenWithNullBakeryId_noXBakeryIdHeader() throws Exception {
        JwtClaims claims = new JwtClaims("admin@test.com", USER_ID, null, "SUPER_ADMIN");
        ServerRequest request = buildRequest("/api/admin/users", "Bearer admin-token");
        when(jwtUtil.parse("admin-token")).thenReturn(claims);
        when(next.handle(any())).thenAnswer(inv -> {
            ServerRequest forwarded = inv.getArgument(0);
            assertThat(forwarded.headers().firstHeader("X-User-Id")).isEqualTo(USER_ID.toString());
            assertThat(forwarded.headers().firstHeader("X-Role")).isEqualTo("SUPER_ADMIN");
            assertThat(forwarded.headers().firstHeader("X-Bakery-Id")).isNull();
            return okResponse;
        });

        ServerResponse result = filter.filter(request, next);

        assertThat(result).isSameAs(okResponse);
    }
}
