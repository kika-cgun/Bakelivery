package com.piotrcapecki.bakelivery.maps.controller;

import com.piotrcapecki.bakelivery.maps.dto.GeocodeResponse;
import com.piotrcapecki.bakelivery.maps.service.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class GeocodingControllerTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.url",
                () -> "redis://localhost:" + redis.getMappedPort(6379));
        registry.add("maps.nominatim.url", () -> "http://localhost:9999");
        registry.add("maps.osrm.url", () -> "http://localhost:9999");
    }

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @MockitoBean
    GeocodingService geocodingService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void geocode_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/internal/maps/geocode")
                .contentType("application/json")
                .content("{\"address\":\"Warszawa\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void geocode_withAuth_returns200() throws Exception {
        when(geocodingService.geocode(any()))
                .thenReturn(new GeocodeResponse(52.23, 21.01, "Warszawa, Polska", false));

        mockMvc.perform(post("/internal/maps/geocode")
                .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                .header("X-Role", "CUSTOMER")
                .contentType("application/json")
                .content("{\"address\":\"Warszawa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(52.23))
                .andExpect(jsonPath("$.cached").value(false));
    }

    @Test
    void geocode_missingAddress_returns400() throws Exception {
        mockMvc.perform(post("/internal/maps/geocode")
                .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                .header("X-Role", "CUSTOMER")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void geocode_shortAddress_returns400() throws Exception {
        mockMvc.perform(post("/internal/maps/geocode")
                .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                .header("X-Role", "CUSTOMER")
                .contentType("application/json")
                .content("{\"address\":\"AB\"}"))
                .andExpect(status().isBadRequest());
    }
}
