package com.piotrcapecki.bakelivery.maps.service;

import com.piotrcapecki.bakelivery.maps.client.NominatimClient;
import com.piotrcapecki.bakelivery.maps.dto.GeocodeRequest;
import com.piotrcapecki.bakelivery.maps.dto.GeocodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@Slf4j
public class GeocodingService {

    private final NominatimClient nominatimClient;
    private final StringRedisTemplate redis;
    private final CacheKeyService cacheKeyService;
    private final ObjectMapper objectMapper;
    private final long geocodingTtlDays;

    public GeocodingService(NominatimClient nominatimClient,
                            StringRedisTemplate redis,
                            CacheKeyService cacheKeyService,
                            ObjectMapper objectMapper,
                            @Value("${cache.geocoding-ttl-days:30}") long geocodingTtlDays) {
        this.nominatimClient = nominatimClient;
        this.redis = redis;
        this.cacheKeyService = cacheKeyService;
        this.objectMapper = objectMapper;
        this.geocodingTtlDays = geocodingTtlDays;
    }

    public GeocodeResponse geocode(GeocodeRequest request) {
        String key = cacheKeyService.geocodeKey(request.address());

        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            log.debug("Geocoding cache HIT dla: {}", request.address());
            GeocodeResponse cachedResponse = deserializeGeocodeResponse(cached);
            if (cachedResponse != null) {
                return new GeocodeResponse(cachedResponse.lat(), cachedResponse.lon(), cachedResponse.displayName(), true);
            }
            // Corrupt cache entry — fall through to re-query
        }

        log.info("Geocoding cache MISS — Nominatim query: {}", request.address());
        NominatimClient.NominatimResult result = nominatimClient.search(request.address());

        double lat;
        double lon;
        try {
            lat = Double.parseDouble(result.lat());
            lon = Double.parseDouble(result.lon());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Nominatim zwrócił nieprawidłowe współrzędne dla: " + request.address(), e);
        }

        GeocodeResponse response = new GeocodeResponse(lat, lon, result.displayName(), false);

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(response),
                Duration.ofDays(geocodingTtlDays));
        } catch (Exception e) {
            log.warn("Nie można zapisać geocodingu do Redis: {}", e.getMessage());
        }

        return response;
    }

    private GeocodeResponse deserializeGeocodeResponse(String json) {
        try {
            return objectMapper.readValue(json, GeocodeResponse.class);
        } catch (Exception e) {
            log.error("Błąd deserializacji geocodingu z Redis, wpis zostanie zignorowany: {}", e.getMessage());
            return null;
        }
    }
}
