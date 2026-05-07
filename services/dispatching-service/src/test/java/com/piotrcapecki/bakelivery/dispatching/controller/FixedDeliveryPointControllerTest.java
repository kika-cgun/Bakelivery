package com.piotrcapecki.bakelivery.dispatching.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.dispatching.config.AppConfig;
import com.piotrcapecki.bakelivery.dispatching.dto.CreateFixedPointRequest;
import com.piotrcapecki.bakelivery.dispatching.dto.FixedPointResponse;
import com.piotrcapecki.bakelivery.dispatching.security.DispatchPrincipal;
import com.piotrcapecki.bakelivery.dispatching.service.FixedDeliveryPointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(AppConfig.class)
class FixedDeliveryPointControllerTest {

    @Autowired WebApplicationContext webApplicationContext;
    @MockitoBean FixedDeliveryPointService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken adminToken(UUID bakeryId) {
        DispatchPrincipal p = new DispatchPrincipal(UUID.randomUUID(), "admin@x.pl", bakeryId, "BAKERY_ADMIN");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_BAKERY_ADMIN")));
    }

    @Test
    void post_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(post("/api/dispatch/admin/fixed-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void post_validRequest_returns201() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        FixedPointResponse resp = new FixedPointResponse(UUID.randomUUID(), "Sklep A",
                "ul. Główna 1", 50.0, 20.0, (short) 31, null, true);
        when(service.create(eq(bakeryId), any())).thenReturn(
                com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint.builder()
                        .id(resp.id()).bakeryId(bakeryId).name("Sklep A")
                        .address("ul. Główna 1").deliveryDays((short) 31).build());

        mockMvc.perform(post("/api/dispatch/admin/fixed-points")
                        .with(authentication(adminToken(bakeryId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFixedPointRequest("Sklep A", "ul. Główna 1", 50.0, 20.0, (short) 31, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sklep A"));
    }

    @Test
    void delete_returns204() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID pointId = UUID.randomUUID();

        mockMvc.perform(delete("/api/dispatch/admin/fixed-points/{id}", pointId)
                        .with(authentication(adminToken(bakeryId))))
                .andExpect(status().isNoContent());
    }
}
