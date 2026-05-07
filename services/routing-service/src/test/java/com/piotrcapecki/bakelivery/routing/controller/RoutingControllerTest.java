package com.piotrcapecki.bakelivery.routing.controller;

import com.piotrcapecki.bakelivery.routing.config.AppConfig;
import com.piotrcapecki.bakelivery.routing.security.RoutingPrincipal;
import com.piotrcapecki.bakelivery.routing.service.RoutingPlanService;
import com.piotrcapecki.bakelivery.routing.repository.RoutePlanRepository;
import com.piotrcapecki.bakelivery.routing.repository.RouteStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(AppConfig.class)
class RoutingControllerTest {

    @Autowired WebApplicationContext context;
    @MockitoBean RoutingPlanService planService;
    @MockitoBean RoutePlanRepository planRepo;
    @MockitoBean RouteStopRepository stopRepo;
    @MockitoBean RedissonClient redissonClient;
    @MockitoBean RabbitTemplate rabbitTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken adminToken(UUID bakeryId) {
        return new UsernamePasswordAuthenticationToken(
                new RoutingPrincipal(UUID.randomUUID(), "a@b.com", bakeryId, "BAKERY_ADMIN"),
                null, List.of(new SimpleGrantedAuthority("ROLE_BAKERY_ADMIN")));
    }

    private UsernamePasswordAuthenticationToken driverToken(UUID bakeryId) {
        return new UsernamePasswordAuthenticationToken(
                new RoutingPrincipal(UUID.randomUUID(), "d@b.com", bakeryId, "DRIVER"),
                null, List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
    }

    @Test
    void postOptimize_asBakeryAdmin_returns202() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        mockMvc.perform(post("/api/routing/admin/optimize")
                        .with(authentication(adminToken(bakeryId)))
                        .param("date", "2026-05-07"))
                .andExpect(status().isAccepted());
    }

    @Test
    void postOptimize_asDriver_returns403() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        mockMvc.perform(post("/api/routing/admin/optimize")
                        .with(authentication(driverToken(bakeryId)))
                        .param("date", "2026-05-07"))
                .andExpect(status().isForbidden());
    }
}
