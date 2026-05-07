package com.piotrcapecki.bakelivery.customer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MapsClient {

    private final RestClient restClient;

    public MapsClient(@Value("${maps.service.url:http://localhost:8089}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public GeocodeResult geocode(GeocodePayload payload) {
        return restClient.post()
                .uri("/internal/maps/geocode")
                .body(payload)
                .retrieve()
                .body(GeocodeResult.class);
    }

    public record GeocodePayload(String address) {}

    public record GeocodeResult(double lat, double lon, String displayName, boolean cached) {}
}
