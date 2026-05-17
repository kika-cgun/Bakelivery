package com.piotrcapecki.bakelivery.common.logging;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MdcLoggingInterceptorTest {

    private final MdcLoggingInterceptor interceptor = new MdcLoggingInterceptor();

    @Test
    void preHandle_populatesMdcFromHeaders() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Request-Id", "req-123");
        req.addHeader("X-Bakery-Id",  "bakery-456");
        req.addHeader("X-User-Id",    "user-789");
        HttpServletResponse res = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(req, res, new Object());

        assertThat(result).isTrue();
        assertThat(MDC.get("traceId")).isEqualTo("req-123");
        assertThat(MDC.get("bakeryId")).isEqualTo("bakery-456");
        assertThat(MDC.get("userId")).isEqualTo("user-789");
    }

    @Test
    void afterCompletion_clearsMdc() throws Exception {
        MDC.put("traceId", "something");
        interceptor.afterCompletion(new MockHttpServletRequest(),
                                    new MockHttpServletResponse(), new Object(), null);
        assertThat(MDC.get("traceId")).isNull();
    }
}
