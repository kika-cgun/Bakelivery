package com.piotrcapecki.bakelivery.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.customer.config.AppConfig;
import com.piotrcapecki.bakelivery.customer.dto.UpsertProfileRequest;
import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.model.CustomerType;
import com.piotrcapecki.bakelivery.customer.security.CustomerPrincipal;
import com.piotrcapecki.bakelivery.customer.service.CustomerProfileService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AppConfig.class)
class CustomerProfileControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private CustomerProfileService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken authToken(CustomerPrincipal p) {
        return new UsernamePasswordAuthenticationToken(p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role())));
    }

    @Test
    void get_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/api/customer/profile"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void get_nonCustomerRole_returns403() throws Exception {
        CustomerPrincipal p = new CustomerPrincipal(UUID.randomUUID(), "x@x.pl", UUID.randomUUID(), "DISPATCHER");
        mockMvc.perform(get("/api/customer/profile").with(authentication(authToken(p))))
                .andExpect(status().isForbidden());
    }

    @Test
    void get_customerWithProfile_returnsProfile() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        when(service.getProfile(uid, bid)).thenReturn(
                Customer.builder().id(UUID.randomUUID()).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).firstName("Anna").build());
        CustomerPrincipal p = new CustomerPrincipal(uid, "anna@x.pl", bid, "CUSTOMER");

        mockMvc.perform(get("/api/customer/profile").with(authentication(authToken(p))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.type").value("INDIVIDUAL"));
    }

    @Test
    void get_customerWithoutProfile_returns404() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        when(service.getProfile(uid, bid)).thenThrow(new NotFoundException("Profile not found"));
        CustomerPrincipal p = new CustomerPrincipal(uid, "anna@x.pl", bid, "CUSTOMER");

        mockMvc.perform(get("/api/customer/profile").with(authentication(authToken(p))))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_validIndividual_returnsProfile() throws Exception {
        UUID uid = UUID.randomUUID();
        UUID bid = UUID.randomUUID();
        when(service.upsertProfile(eq(uid), eq(bid), any())).thenReturn(
                Customer.builder().id(UUID.randomUUID()).userId(uid).bakeryId(bid).type(CustomerType.INDIVIDUAL).firstName("Anna").build());
        CustomerPrincipal p = new CustomerPrincipal(uid, "anna@x.pl", bid, "CUSTOMER");

        mockMvc.perform(put("/api/customer/profile")
                        .with(authentication(authToken(p)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpsertProfileRequest(CustomerType.INDIVIDUAL, "Anna", "Kowalska", "+48 600 000 000", null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"));

        verify(service).upsertProfile(eq(uid), eq(bid), any(UpsertProfileRequest.class));
    }
}
