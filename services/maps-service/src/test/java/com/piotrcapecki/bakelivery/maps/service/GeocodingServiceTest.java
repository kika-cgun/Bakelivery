package com.piotrcapecki.bakelivery.maps.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.piotrcapecki.bakelivery.maps.dto.GeocodeRequest;
import com.piotrcapecki.bakelivery.maps.dto.GeocodeResponse;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class GeocodingServiceTest {

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
        registry.add("maps.nominatim.url", wireMock::baseUrl);
        registry.add("maps.osrm.url", () -> "http://localhost:9999");
    }

    @Autowired
    GeocodingService geocodingService;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void geocode_missThenCacheHit() {
        wireMock.stubFor(get(urlPathEqualTo("/search"))
            .willReturn(okJson("""
                [{"lat":"52.2297","lon":"21.0122","display_name":"Warszawa, Polska"}]
                """)));

        GeocodeResponse first = geocodingService.geocode(new GeocodeRequest("Warszawa"));
        assertThat(first.lat()).isEqualTo(52.2297);
        assertThat(first.lon()).isEqualTo(21.0122);
        assertThat(first.cached()).isFalse();

        GeocodeResponse second = geocodingService.geocode(new GeocodeRequest("Warszawa"));
        assertThat(second.cached()).isTrue();

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void geocode_unknownAddress_throwsNotFoundException() {
        wireMock.stubFor(get(urlPathEqualTo("/search"))
            .willReturn(okJson("[]")));

        assertThatThrownBy(() -> geocodingService.geocode(new GeocodeRequest("XYZ NIEZNANY ADRES")))
            .isInstanceOf(NotFoundException.class);
    }
}
