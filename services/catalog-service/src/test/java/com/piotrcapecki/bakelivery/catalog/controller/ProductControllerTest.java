package com.piotrcapecki.bakelivery.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.catalog.config.AppConfig;
import com.piotrcapecki.bakelivery.catalog.dto.CreateProductRequest;
import com.piotrcapecki.bakelivery.catalog.dto.ProductResponse;
import com.piotrcapecki.bakelivery.catalog.dto.UpdateProductRequest;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.ProductService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(AppConfig.class)
class ProductControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @MockitoBean private ProductService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private UsernamePasswordAuthenticationToken authToken(CatalogPrincipal p) {
        return new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_" + p.role())));
    }

    // 1. POST as BAKERY_ADMIN → 201
    @Test
    void post_asBakeryAdmin_returns201() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        ProductResponse response = new ProductResponse(productId, null, "SKU-1", "croissant", "Croissant",
                "Buttery", new BigDecimal("8.50"), (short) 127, true);
        when(service.create(eq(bakeryId), any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/catalog/admin/products")
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProductRequest(null, "SKU-1", "croissant", "Croissant",
                                        "Buttery", new BigDecimal("8.50"), (short) 127))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("croissant"));
    }

    // 2. POST duplicate slug → 409
    @Test
    void post_duplicateSlug_returns409() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.create(eq(bakeryId), any(CreateProductRequest.class)))
                .thenThrow(new ConflictException("Product slug 'croissant' already exists"));

        mockMvc.perform(post("/api/catalog/admin/products")
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProductRequest(null, "SKU-1", "croissant", "Croissant",
                                        null, new BigDecimal("8.50"), null))))
                .andExpect(status().isConflict());
    }

    // 3. POST as CUSTOMER → 403
    @Test
    void post_asCustomer_returns403() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal customer = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");

        mockMvc.perform(post("/api/catalog/admin/products")
                        .with(authentication(authToken(customer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProductRequest(null, null, "croissant", "Croissant",
                                        null, new BigDecimal("8.50"), null))))
                .andExpect(status().isForbidden());
    }

    // 4. PATCH/{id} → 200
    @Test
    void patch_existingProduct_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        ProductResponse response = new ProductResponse(productId, null, "SKU-1", "croissant", "Croissant XL",
                "Buttery", new BigDecimal("9.00"), (short) 127, true);
        when(service.update(eq(bakeryId), eq(productId), any(UpdateProductRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/catalog/admin/products/" + productId)
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProductRequest(null, null, null, "Croissant XL",
                                        null, new BigDecimal("9.00"), null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Croissant XL"))
                .andExpect(jsonPath("$.basePrice").value(9.00));
    }

    // 5. DELETE/{id} → 204 (soft delete)
    @Test
    void delete_existingProduct_returns204() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        mockMvc.perform(delete("/api/catalog/admin/products/" + productId)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isNoContent());
    }

    // 6. GET/{id} missing product → 404
    @Test
    void get_missingProduct_returns404() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.get(eq(bakeryId), eq(productId)))
                .thenThrow(new NotFoundException("Product not found"));

        mockMvc.perform(get("/api/catalog/admin/products/" + productId)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isNotFound());
    }
}
