package com.piotrcapecki.bakelivery.maps.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.piotrcapecki.bakelivery.maps.dto.Coordinate;
import com.piotrcapecki.bakelivery.maps.dto.MatrixRequest;
import com.piotrcapecki.bakelivery.maps.dto.MatrixResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class OsrmServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.url",
            () -> "redis://localhost:" + redis.getMappedPort(6379));
        registry.add("maps.osrm.url", wireMock::baseUrl);
        registry.add("maps.nominatim.url", () -> "http://localhost:9999");
    }

    @Autowired
    OsrmService osrmService;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void matrix_missThenCacheHit() {
        wireMock.stubFor(get(urlMatching("/table/v1/driving/.*"))
            .willReturn(okJson("""
                {
                  "code": "Ok",
                  "durations": [[0.0, 120.5], [130.2, 0.0]],
                  "distances": [[0.0, 2500.0], [2600.0, 0.0]]
                }
                """)));

        List<Coordinate> coords = List.of(
            new Coordinate(52.229, 21.012),
            new Coordinate(52.200, 21.050)
        );
        MatrixRequest request = new MatrixRequest(coords, coords);

        MatrixResponse first = osrmService.matrix(request);
        assertThat(first.durations()[0][1]).isEqualTo(120.5);
        assertThat(first.cached()).isFalse();

        MatrixResponse second = osrmService.matrix(request);
        assertThat(second.cached()).isTrue();

        wireMock.verify(1, getRequestedFor(urlMatching("/table/v1/driving/.*")));
    }
}
