package com.piotrcapecki.bakelivery.common.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class BakeryContextFilterTest {

    private final BakeryContextFilter filter = new BakeryContextFilter();

    @AfterEach
    void tearDown() {
        BakeryContext.clear();
    }

    @Test
    void invalidBakeryIdHeader_returnsBadRequestAndClearsContext() throws Exception {
        BakeryContext.set(UUID.randomUUID());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Bakery-Id", "not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainCalled.set(true));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("Invalid X-Bakery-Id header");
        assertThat(chainCalled).isFalse();
        assertThat(BakeryContext.get()).isNull();
    }
}
