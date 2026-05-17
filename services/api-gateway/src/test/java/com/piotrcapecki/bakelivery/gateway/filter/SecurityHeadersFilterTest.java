package com.piotrcapecki.bakelivery.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void preHandle_addsXContentTypeOptionsHeader() throws Exception {
        boolean proceed = filter.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void preHandle_addsXFrameOptionsHeader() throws Exception {
        filter.preHandle(request, response, new Object());

        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void preHandle_addsReferrerPolicyHeader() throws Exception {
        filter.preHandle(request, response, new Object());

        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
    }

    @Test
    void preHandle_addsContentSecurityPolicyHeader() throws Exception {
        filter.preHandle(request, response, new Object());

        assertThat(response.getHeader("Content-Security-Policy")).isEqualTo("default-src 'self'");
    }

    @Test
    void preHandle_allSecurityHeadersPresentAndRequestContinues() throws Exception {
        boolean proceed = filter.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("Content-Security-Policy")).isEqualTo("default-src 'self'");
    }
}
