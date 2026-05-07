package com.piotrcapecki.bakelivery.maps.service;

import tools.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.maps.client.NominatimClient;
import com.piotrcapecki.bakelivery.maps.dto.GeocodeRequest;
import com.piotrcapecki.bakelivery.maps.dto.GeocodeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final NominatimClient nominatimClient;
    private final StringRedisTemplate redis;
    private final CacheKeyService cacheKeyService;
    private final ObjectMapper objectMapper;

    @Value("${cache.geocoding-ttl-days:30}")
    private long geocodingTtlDays;

    public GeocodeResponse geocode(GeocodeRequest request) {
        String key = cacheKeyService.geocodeKey(request.address());

        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            log.debug("Geocoding cache HIT dla: {}", request.address());
            return deserializeGeocodeResponse(cached, true);
        }

        log.info("Geocoding cache MISS — Nominatim query: {}", request.address());
        NominatimClient.NominatimResult result = nominatimClient.search(request.address());

        GeocodeResponse response = new GeocodeResponse(
            Double.parseDouble(result.lat()),
            Double.parseDouble(result.lon()),
            result.displayName(),
            false
        );

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(response),
                Duration.ofDays(geocodingTtlDays));
        } catch (Exception e) {
            log.warn("Nie można zapisać geocodingu do Redis: {}", e.getMessage());
        }

        return response;
    }

    private GeocodeResponse deserializeGeocodeResponse(String json, boolean cached) {
        try {
            GeocodeResponse base = objectMapper.readValue(json, GeocodeResponse.class);
            return new GeocodeResponse(base.lat(), base.lon(), base.displayName(), cached);
        } catch (Exception e) {
            log.error("Błąd deserializacji geocodingu z Redis: {}", e.getMessage());
            return null;
        }
    }
}
