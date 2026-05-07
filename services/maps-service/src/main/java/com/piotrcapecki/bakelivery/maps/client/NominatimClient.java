package com.piotrcapecki.bakelivery.maps.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class NominatimClient {

    private final RestClient restClient;

    public NominatimClient(@Qualifier("nominatimRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public NominatimResult search(String address) {
        List<NominatimResult> results = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/search")
                .queryParam("q", address)
                .queryParam("format", "jsonv2")
                .queryParam("limit", "1")
                .queryParam("addressdetails", "1")
                .build())
            .retrieve()
            .body(new ParameterizedTypeReference<List<NominatimResult>>() {});

        if (results == null || results.isEmpty()) {
            throw new NotFoundException("Nie można zgeokodować adresu: " + address);
        }
        return results.getFirst();
    }

    public record NominatimResult(
        @JsonProperty("lat") String lat,
        @JsonProperty("lon") String lon,
        @JsonProperty("display_name") String displayName
    ) {}
}
