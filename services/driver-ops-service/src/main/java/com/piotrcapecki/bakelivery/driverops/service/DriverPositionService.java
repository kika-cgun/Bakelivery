package com.piotrcapecki.bakelivery.driverops.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverPositionService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void update(UUID driverId, UUID bakeryId, UUID shiftId, double lat, double lon) {
        Map<String, Object> payload = Map.of(
                "lat", lat,
                "lon", lon,
                "timestamp", Instant.now().toString(),
                "bakeryId", bakeryId.toString(),
                "shiftId", shiftId.toString()
        );

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize position payload", e);
            return;
        }

        String key = "driver:pos:" + driverId;
        redis.opsForValue().set(key, json, Duration.ofSeconds(60));
        redis.convertAndSend("driver.pos." + driverId, json);

        log.debug("Updated position for driver {}: lat={}, lon={}", driverId, lat, lon);
    }
}
