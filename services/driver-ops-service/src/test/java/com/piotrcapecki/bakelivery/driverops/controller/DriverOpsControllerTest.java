package com.piotrcapecki.bakelivery.driverops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.driverops.config.AppConfig;
import com.piotrcapecki.bakelivery.driverops.dto.ShiftResponse;
import com.piotrcapecki.bakelivery.driverops.dto.SkipStopRequest;
import com.piotrcapecki.bakelivery.driverops.dto.StartShiftRequest;
import com.piotrcapecki.bakelivery.driverops.messaging.DeliveryEventPublisher;
import com.piotrcapecki.bakelivery.driverops.security.DriverOpsPrincipal;
import com.piotrcapecki.bakelivery.driverops.service.DriverPositionService;
import com.piotrcapecki.bakelivery.driverops.service.ProofOfDeliveryService;
import com.piotrcapecki.bakelivery.driverops.service.ShiftService;
import com.piotrcapecki.bakelivery.driverops.service.StopProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(AppConfig.class)
class DriverOpsControllerTest {

    @Autowired
    WebApplicationContext context;

    @MockitoBean ConnectionFactory connectionFactory;
    @MockitoBean ShiftService shiftService;
    @MockitoBean StopProgressService stopProgressService;
    @MockitoBean ProofOfDeliveryService proofService;
    @MockitoBean DriverPositionService positionService;
    @MockitoBean DeliveryEventPublisher eventPublisher;
    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean S3Client s3Client;
    @MockitoBean S3Presigner s3Presigner;

    MockMvc mockMvc;

    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID BAKERY_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void startShift_returns201ForDriver() throws Exception {
        UUID planId = UUID.randomUUID();
        ShiftResponse response = new ShiftResponse(UUID.randomUUID(), DRIVER_ID, LocalDate.now(),
                planId, "ACTIVE", 0, LocalDateTime.now(), null);
        when(shiftService.startShift(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/driver-ops/shift/start")
                        .with(authentication(driverAuth()))
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new StartShiftRequest(planId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void startShift_returns403ForBakeryAdmin() throws Exception {
        UUID planId = UUID.randomUUID();

        mockMvc.perform(post("/api/driver-ops/shift/start")
                        .with(authentication(bakeryAdminAuth()))
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new StartShiftRequest(planId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShifts_returns200ForBakeryAdmin() throws Exception {
        when(shiftService.getShiftsForBakery(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/driver-ops/admin/shifts")
                        .with(authentication(bakeryAdminAuth())))
                .andExpect(status().isOk());
    }

    @Test
    void adminShifts_returns403ForDriver() throws Exception {
        mockMvc.perform(get("/api/driver-ops/admin/shifts")
                        .with(authentication(driverAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void skipStop_returns404WhenStopNotInShift() throws Exception {
        when(stopProgressService.skipStop(any(), any(), any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Stop not found in current shift"));

        mockMvc.perform(post("/api/driver-ops/shift/stops/{stopId}/skip", UUID.randomUUID())
                        .with(authentication(driverAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new SkipStopRequest("Not home"))))
                .andExpect(status().isNotFound());
    }

    private UsernamePasswordAuthenticationToken driverAuth() {
        DriverOpsPrincipal principal = new DriverOpsPrincipal(DRIVER_ID, "driver@example.com", BAKERY_ID, "DRIVER");
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
    }

    private UsernamePasswordAuthenticationToken bakeryAdminAuth() {
        DriverOpsPrincipal principal = new DriverOpsPrincipal(UUID.randomUUID(), "admin@example.com", BAKERY_ID, "BAKERY_ADMIN");
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_BAKERY_ADMIN")));
    }
}
