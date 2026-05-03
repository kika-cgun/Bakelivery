package com.piotrcapecki.bakelivery.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.auth.config.AppConfig;
import com.piotrcapecki.bakelivery.auth.dto.CreateBakeryRequest;
import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.service.AuthService;
import com.piotrcapecki.bakelivery.auth.service.BakeryService;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AppConfig.class)
class BakeryControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthService authService;
    @MockitoBean private BakeryService bakeryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createBakery_rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/admin/platform/bakeries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isForbidden());

        verify(bakeryService, never()).createBakeryWithFirstAdmin(any());
    }

    @Test
    @WithMockUser(roles = "BAKERY_ADMIN")
    void createBakery_rejectsBakeryAdminRole() throws Exception {
        mockMvc.perform(post("/api/admin/platform/bakeries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isForbidden());

        verify(bakeryService, never()).createBakeryWithFirstAdmin(any());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createBakery_allowsSuperAdmin() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        Bakery bakery = Bakery.builder()
                .id(bakeryId)
                .name("Sweet Rolls")
                .slug("sweet-rolls")
                .build();
        when(bakeryService.createBakeryWithFirstAdmin(any(CreateBakeryRequest.class))).thenReturn(bakery);

        mockMvc.perform(post("/api/admin/platform/bakeries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(bakeryId.toString()))
                .andExpect(jsonPath("$.name").value("Sweet Rolls"))
                .andExpect(jsonPath("$.slug").value("sweet-rolls"));

        verify(bakeryService).createBakeryWithFirstAdmin(any(CreateBakeryRequest.class));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createBakery_returns400OnInvalidRequest() throws Exception {
        String invalidBody = objectMapper.writeValueAsString(new CreateBakeryRequest(
                "", // blank name
                "ok-slug",
                "contact@bakery.pl", null,
                "admin@bakery.pl", "securePass1"
        ));

        mockMvc.perform(post("/api/admin/platform/bakeries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());

        verify(bakeryService, never()).createBakeryWithFirstAdmin(any());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createBakery_returns400OnInvalidSlug() throws Exception {
        String invalidBody = objectMapper.writeValueAsString(new CreateBakeryRequest(
                "Sweet Rolls",
                "INVALID SLUG!", // uppercase + spaces not allowed
                "contact@bakery.pl", null,
                "admin@bakery.pl", "securePass1"
        ));

        mockMvc.perform(post("/api/admin/platform/bakeries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());

        verify(bakeryService, never()).createBakeryWithFirstAdmin(any());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createBakery_returns409WhenSlugExists() throws Exception {
        when(bakeryService.createBakeryWithFirstAdmin(any(CreateBakeryRequest.class)))
                .thenThrow(new ConflictException("Slug already in use"));

        mockMvc.perform(post("/api/admin/platform/bakeries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createBakery_returns409WhenEmailExists() throws Exception {
        when(bakeryService.createBakeryWithFirstAdmin(any(CreateBakeryRequest.class)))
                .thenThrow(new ConflictException("Email already in use"));

        mockMvc.perform(post("/api/admin/platform/bakeries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict());
    }

    private String validRequestJson() throws Exception {
        return objectMapper.writeValueAsString(new CreateBakeryRequest(
                "Sweet Rolls", "sweet-rolls",
                "contact@sweet-rolls.pl", "+48123456789",
                "admin@sweet-rolls.pl", "securePass1"
        ));
    }
}
