package com.piotrcapecki.bakelivery.maps.service;

import tools.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.maps.dto.Coordinate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheKeyService {

    private static final String GEO_PREFIX = "geo:";
    private static final String MATRIX_PREFIX = "osrm:matrix:";

    private final ObjectMapper objectMapper;

    public String geocodeKey(String address) {
        String normalized = Normalizer.normalize(address.toLowerCase().trim(), Normalizer.Form.NFC)
            .replaceAll("\\s+", " ");
        return GEO_PREFIX + normalized;
    }

    public String matrixKey(List<Coordinate> sources, List<Coordinate> destinations) {
        try {
            List<Coordinate> sortedSrc = sources.stream()
                .sorted(Comparator.comparingDouble(Coordinate::lat)
                    .thenComparingDouble(Coordinate::lon))
                .toList();
            List<Coordinate> sortedDst = destinations.stream()
                .sorted(Comparator.comparingDouble(Coordinate::lat)
                    .thenComparingDouble(Coordinate::lon))
                .toList();
            String json = objectMapper.writeValueAsString(
                new MatrixKeyPayload(sortedSrc, sortedDst)
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return MATRIX_PREFIX + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Nie można wygenerować klucza cache dla macierzy", e);
        }
    }

    private record MatrixKeyPayload(List<Coordinate> sources, List<Coordinate> destinations) {}
}
