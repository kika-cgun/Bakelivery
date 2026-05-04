package com.piotrcapecki.bakelivery.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.customer.config.AppConfig;
import com.piotrcapecki.bakelivery.customer.dto.CreateAddressRequest;
import com.piotrcapecki.bakelivery.customer.dto.UpdateAddressRequest;
import com.piotrcapecki.bakelivery.customer.model.DeliveryAddress;
import com.piotrcapecki.bakelivery.customer.security.CustomerPrincipal;
import com.piotrcapecki.bakelivery.customer.service.DeliveryAddressService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AppConfig.class)
class DeliveryAddressControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private DeliveryAddressService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken authToken(CustomerPrincipal p) {
        return new UsernamePasswordAuthenticationToken(p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role())));
    }

    private CustomerPrincipal customerPrincipal(UUID uid, UUID bid) {
        return new CustomerPrincipal(uid, "anna@x.pl", bid, "CUSTOMER");
    }

    @Test
    void list_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/api/customer/addresses"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void list_nonCustomerRole_returns403() throws Exception {
        CustomerPrincipal p = new CustomerPrincipal(UUID.randomUUID(), "x@x.pl", UUID.randomUUID(), "DISPATCHER");
        mockMvc.perform(get("/api/customer/addresses").with(authentication(authToken(p))))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_customer_returnsAddresses() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        DeliveryAddress a = DeliveryAddress.builder().id(UUID.randomUUID()).customerId(cid).bakeryId(bid)
                .label("Dom").street("ul. Pierwsza 1").postalCode("00-001").city("Warszawa").build();
        when(service.list(uid, bid)).thenReturn(List.of(a));

        mockMvc.perform(get("/api/customer/addresses").with(authentication(authToken(customerPrincipal(uid, bid)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].street").value("ul. Pierwsza 1"))
                .andExpect(jsonPath("$[0].label").value("Dom"));
    }

    @Test
    void create_validAddress_returns201() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        DeliveryAddress a = DeliveryAddress.builder().id(UUID.randomUUID()).customerId(cid).bakeryId(bid)
                .street("ul. Nowa 1").postalCode("00-002").city("Kraków").build();
        when(service.create(eq(uid), eq(bid), any(CreateAddressRequest.class))).thenReturn(a);

        mockMvc.perform(post("/api/customer/addresses")
                        .with(authentication(authToken(customerPrincipal(uid, bid))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAddressRequest(null, "ul. Nowa 1", "00-002", "Kraków", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.street").value("ul. Nowa 1"));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();

        mockMvc.perform(post("/api/customer/addresses")
                        .with(authentication(authToken(customerPrincipal(uid, bid))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"street\":\"\",\"postalCode\":\"00-001\",\"city\":\"Warszawa\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingAddress_returns200() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        DeliveryAddress updated = DeliveryAddress.builder().id(addressId).customerId(cid).bakeryId(bid)
                .street("ul. Zmieniona 5").postalCode("00-010").city("Gdańsk").build();
        when(service.update(eq(uid), eq(bid), eq(addressId), any(UpdateAddressRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/customer/addresses/" + addressId)
                        .with(authentication(authToken(customerPrincipal(uid, bid))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateAddressRequest(null, "ul. Zmieniona 5", "00-010", "Gdańsk", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("ul. Zmieniona 5"));
    }

    @Test
    void update_addressNotFound_returns404() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(service.update(eq(uid), eq(bid), eq(addressId), any())).thenThrow(new NotFoundException("Address not found"));

        mockMvc.perform(put("/api/customer/addresses/" + addressId)
                        .with(authentication(authToken(customerPrincipal(uid, bid))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateAddressRequest(null, "ul. X 1", "00-000", "Warszawa", null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingAddress_returns204() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        mockMvc.perform(delete("/api/customer/addresses/" + addressId)
                        .with(authentication(authToken(customerPrincipal(uid, bid)))))
                .andExpect(status().isNoContent());
    }
}
