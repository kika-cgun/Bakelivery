package com.piotrcapecki.bakelivery.dispatching.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.dispatching.config.AppConfig;
import com.piotrcapecki.bakelivery.dispatching.dto.AssignDriverRequest;
import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import com.piotrcapecki.bakelivery.dispatching.model.DispatchStopStatus;
import com.piotrcapecki.bakelivery.dispatching.security.DispatchPrincipal;
import com.piotrcapecki.bakelivery.dispatching.service.DispatchStopService;
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

import java.time.LocalDate;
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
class DispatchStopControllerTest {

    @Autowired WebApplicationContext webApplicationContext;
    @MockitoBean DispatchStopService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken dispatcherToken(UUID bakeryId) {
        DispatchPrincipal p = new DispatchPrincipal(UUID.randomUUID(), "d@x.pl", bakeryId, "DISPATCHER");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_DISPATCHER")));
    }

    @Test
    void getStops_returnsListForDate() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 7);
        DispatchStop stop = DispatchStop.builder()
                .id(UUID.randomUUID()).bakeryId(bakeryId).date(date)
                .customerName("Anna").deliveryAddress("ul. Różana 2")
                .status(DispatchStopStatus.PENDING).build();
        when(service.listByDate(eq(bakeryId), eq(date))).thenReturn(List.of(stop));

        mockMvc.perform(get("/api/dispatch/admin/stops")
                        .param("date", "2026-05-07")
                        .with(authentication(dispatcherToken(bakeryId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("Anna"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void assign_returns404ForWrongBakery() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID stopId = UUID.randomUUID();
        when(service.assignDriver(eq(bakeryId), eq(stopId), any()))
                .thenThrow(new NotFoundException("Dispatch stop not found: " + stopId));

        mockMvc.perform(patch("/api/dispatch/admin/stops/{id}/assign", stopId)
                        .with(authentication(dispatcherToken(bakeryId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssignDriverRequest(UUID.randomUUID(), "Kierowca X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void assign_driverRoleCannotAccessAdminEndpoint() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        DispatchPrincipal driverPrincipal = new DispatchPrincipal(UUID.randomUUID(), "d@x.pl", bakeryId, "DRIVER");
        UsernamePasswordAuthenticationToken driverToken = new UsernamePasswordAuthenticationToken(
                driverPrincipal, null, List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));

        mockMvc.perform(get("/api/dispatch/admin/stops")
                        .param("date", "2026-05-07")
                        .with(authentication(driverToken)))
                .andExpect(status().isForbidden());
    }
}
