package com.piotrcapecki.bakelivery.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Slf4j
public class RateLimitFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final Map<String, Integer> limits;
    private final int defaultLimit;

    public RateLimitFilter(
        StringRedisTemplate redis,
        @Value("${rate-limit.enabled:true}") boolean enabled,
        @Value("#{${rate-limit.limits}}") Map<String, Integer> limits,
        @Value("${rate-limit.limits.default:60}") int defaultLimit
    ) {
        this.redis = redis;
        this.enabled = enabled;
        this.limits = limits;
        this.defaultLimit = defaultLimit;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        if (!enabled) return next.handle(request);

        String userId = request.headers().firstHeader("X-User-Id");
        if (userId == null) return next.handle(request);

        String role = request.headers().firstHeader("X-Role");
        int limit = role != null ? limits.getOrDefault(role, defaultLimit) : defaultLimit;

        long minuteBucket = Instant.now().getEpochSecond() / 60;
        String key = "rl:" + userId + ":" + minuteBucket;

        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, Duration.ofSeconds(70));
        }

        if (count != null && count > limit) {
            log.warn("Rate limit exceeded userId={} role={} count={}", userId, role, count);
            return ServerResponse.status(429)
                .header("X-RateLimit-Limit", String.valueOf(limit))
                .header("X-RateLimit-Remaining", "0")
                .header("Retry-After", "60")
                .build();
        }

        return next.handle(request);
    }
}
