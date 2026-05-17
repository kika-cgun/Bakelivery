package com.piotrcapecki.bakelivery.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

public class MdcLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String requestId = req.getHeader("X-Request-Id");
        String bakeryId  = req.getHeader("X-Bakery-Id");
        String userId    = req.getHeader("X-User-Id");
        if (requestId != null) MDC.put("traceId",  requestId);
        if (bakeryId  != null) MDC.put("bakeryId", bakeryId);
        if (userId    != null) MDC.put("userId",   userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res,
                                Object handler, Exception ex) {
        MDC.clear();
    }
}
