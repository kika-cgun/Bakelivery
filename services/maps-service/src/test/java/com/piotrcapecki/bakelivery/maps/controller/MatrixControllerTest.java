package com.piotrcapecki.bakelivery.maps.controller;

import com.piotrcapecki.bakelivery.maps.dto.MatrixResponse;
import com.piotrcapecki.bakelivery.maps.service.OsrmService;
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
class MatrixControllerTest {

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
    OsrmService osrmService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void matrix_withAuth_returns200() throws Exception {
        when(osrmService.matrix(any()))
                .thenReturn(new MatrixResponse(
                        new double[][]{{0.0, 120.5}, {130.2, 0.0}},
                        new double[][]{{0.0, 2500.0}, {2600.0, 0.0}},
                        false
                ));

        mockMvc.perform(post("/internal/maps/matrix")
                .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                .header("X-Role", "DISPATCHER")
                .contentType("application/json")
                .content("""
                        {
                          "sources": [{"lat": 52.229, "lon": 21.012}],
                          "destinations": [{"lat": 52.200, "lon": 21.050}]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(false));
    }

    @Test
    void matrix_missingSources_returns400() throws Exception {
        mockMvc.perform(post("/internal/maps/matrix")
                .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                .header("X-Role", "DISPATCHER")
                .contentType("application/json")
                .content("{\"destinations\": [{\"lat\": 52.2, \"lon\": 21.0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void matrix_emptySources_returns400() throws Exception {
        mockMvc.perform(post("/internal/maps/matrix")
                .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                .header("X-Role", "DISPATCHER")
                .contentType("application/json")
                .content("{\"sources\": [], \"destinations\": [{\"lat\": 52.2, \"lon\": 21.0}]}"))
                .andExpect(status().isBadRequest());
    }
}
