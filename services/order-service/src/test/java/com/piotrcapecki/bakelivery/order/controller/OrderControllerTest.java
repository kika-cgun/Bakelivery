package com.piotrcapecki.bakelivery.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.order.config.AppConfig;
import com.piotrcapecki.bakelivery.order.dto.*;
import com.piotrcapecki.bakelivery.order.security.OrderPrincipal;
import com.piotrcapecki.bakelivery.order.service.OrderService;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(AppConfig.class)
class OrderControllerTest {

    @Autowired WebApplicationContext webApplicationContext;
    @MockitoBean OrderService orderService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private UsernamePasswordAuthenticationToken authToken(OrderPrincipal p) {
        return new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role())));
    }

    private OrderResponse sampleOrderResponse(UUID bakery, UUID customer) {
        return new OrderResponse(UUID.randomUUID(), bakery, customer, "PLACED",
                new BigDecimal("25.00"), "ul. Testowa 1", null, List.of(), LocalDateTime.now());
    }

    @Test
    void postOrder_asCustomer_returns201() throws Exception {
        UUID bakery = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        OrderPrincipal principal = new OrderPrincipal(customer, "cust@test.com", bakery, "CUSTOMER");

        when(orderService.placeOrder(any(), any(), anyString(), anyString()))
                .thenReturn(sampleOrderResponse(bakery, customer));

        CreateOrderRequest req = new CreateOrderRequest(
                UUID.randomUUID(), "ul. Testowa 1",
                List.of(new OrderItemRequest(UUID.randomUUID(), null, 2)),
                null);

        mockMvc.perform(post("/api/orders")
                        .with(authentication(authToken(principal)))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    void postOrder_duplicateIdempotencyKey_returns409() throws Exception {
        UUID bakery = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        OrderPrincipal principal = new OrderPrincipal(customer, "cust@test.com", bakery, "CUSTOMER");

        when(orderService.placeOrder(any(), any(), anyString(), anyString()))
                .thenThrow(new ConflictException("Duplicate order"));

        CreateOrderRequest req = new CreateOrderRequest(
                UUID.randomUUID(), "ul. Testowa 1",
                List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)),
                null);

        mockMvc.perform(post("/api/orders")
                        .with(authentication(authToken(principal)))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAdminOrders_asCustomer_returns403() throws Exception {
        UUID bakery = UUID.randomUUID();
        OrderPrincipal customer = new OrderPrincipal(UUID.randomUUID(), "cust@test.com", bakery, "CUSTOMER");

        mockMvc.perform(get("/api/orders/admin")
                        .with(authentication(authToken(customer))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAdminOrders_asBakeryAdmin_returns200() throws Exception {
        UUID bakery = UUID.randomUUID();
        OrderPrincipal admin = new OrderPrincipal(UUID.randomUUID(), "admin@bakery.com", bakery, "BAKERY_ADMIN");

        when(orderService.listForAdmin(bakery)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/admin")
                        .with(authentication(authToken(admin))))
                .andExpect(status().isOk());
    }

    @Test
    void getMyOrder_notFound_returns404() throws Exception {
        UUID bakery = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderPrincipal customer = new OrderPrincipal(UUID.randomUUID(), "cust@test.com", bakery, "CUSTOMER");

        when(orderService.getForCustomer(eq(orderId), any(), any()))
                .thenThrow(new NotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/" + orderId)
                        .with(authentication(authToken(customer))))
                .andExpect(status().isNotFound());
    }
}
