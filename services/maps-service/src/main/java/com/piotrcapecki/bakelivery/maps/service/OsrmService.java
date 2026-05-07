package com.piotrcapecki.bakelivery.maps.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.maps.client.OsrmClient;
import com.piotrcapecki.bakelivery.maps.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class OsrmService {

    private final OsrmClient osrmClient;
    private final StringRedisTemplate redis;
    private final CacheKeyService cacheKeyService;
    private final ObjectMapper objectMapper;
    private final long matrixTtlHours;

    public OsrmService(OsrmClient osrmClient,
                       StringRedisTemplate redis,
                       CacheKeyService cacheKeyService,
                       ObjectMapper objectMapper,
                       @Value("${cache.matrix-ttl-hours:24}") long matrixTtlHours) {
        this.osrmClient = osrmClient;
        this.redis = redis;
        this.cacheKeyService = cacheKeyService;
        this.objectMapper = objectMapper;
        this.matrixTtlHours = matrixTtlHours;
    }

    public MatrixResponse matrix(MatrixRequest request) {
        String key = cacheKeyService.matrixKey(request.sources(), request.destinations());

        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            log.debug("Matrix cache HIT, key={}", key);
            MatrixResponse cachedResponse = deserializeMatrixResponse(cached);
            if (cachedResponse != null) {
                return new MatrixResponse(cachedResponse.durations(), cachedResponse.distances(), true);
            }
            // Corrupt cache entry — fall through to re-query
        }

        log.info("Matrix cache MISS — OSRM table query, {} sources × {} destinations",
            request.sources().size(), request.destinations().size());
        OsrmClient.OsrmTableResponse osrmResponse =
            osrmClient.table(request.sources(), request.destinations());

        MatrixResponse response = new MatrixResponse(
            osrmResponse.durations(),
            osrmResponse.distances(),
            false
        );

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(response),
                Duration.ofHours(matrixTtlHours));
        } catch (Exception e) {
            log.warn("Nie można zapisać matrix do Redis: {}", e.getMessage());
        }

        return response;
    }

    public RouteResponse route(RouteRequest request) {
        OsrmClient.OsrmRouteResponse osrmResponse =
            osrmClient.route(request.waypoints(), request.steps(), request.overview());

        if (osrmResponse == null || osrmResponse.routes() == null || osrmResponse.routes().isEmpty()) {
            throw new NotFoundException("OSRM nie zwrócił trasy dla podanych punktów");
        }

        OsrmClient.OsrmRouteResponse.OsrmRoute route = osrmResponse.routes().getFirst();
        String geometryJson = null;
        if (request.overview() && route.geometry() != null) {
            try {
                geometryJson = objectMapper.writeValueAsString(route.geometry());
            } catch (Exception e) {
                log.warn("Nie można serializować geometry GeoJSON: {}", e.getMessage());
            }
        }

        List<RouteResponse.RouteStep> steps = null;
        if (request.steps() && route.legs() != null) {
            steps = route.legs().stream()
                .flatMap(leg -> leg.steps() != null ? leg.steps().stream() : Stream.empty())
                .map(s -> new RouteResponse.RouteStep(
                    s.distance(), s.duration(),
                    s.maneuver() != null ? s.maneuver().type() : null,
                    s.name()
                ))
                .toList();
        }

        return new RouteResponse(route.distance(), route.duration(), geometryJson, steps);
    }

    private MatrixResponse deserializeMatrixResponse(String json) {
        try {
            return objectMapper.readValue(json, MatrixResponse.class);
        } catch (Exception e) {
            log.error("Błąd deserializacji matrix z Redis, wpis zostanie zignorowany: {}", e.getMessage());
            return null;
        }
    }
}
