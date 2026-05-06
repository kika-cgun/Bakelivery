package com.piotrcapecki.bakelivery.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.catalog.config.AppConfig;
import com.piotrcapecki.bakelivery.catalog.dto.CreateVariantRequest;
import com.piotrcapecki.bakelivery.catalog.dto.UpdateVariantRequest;
import com.piotrcapecki.bakelivery.catalog.dto.VariantResponse;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.ProductVariantService;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
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

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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
class ProductVariantControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @MockitoBean private ProductVariantService service;

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
        UUID variantId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.create(eq(bakeryId), eq(productId), any(CreateVariantRequest.class)))
                .thenReturn(new VariantResponse(variantId, "Large", null, new BigDecimal("2.00"), 1));

        mockMvc.perform(post("/api/catalog/admin/products/" + productId + "/variants")
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateVariantRequest("Large", null, new BigDecimal("2.00"), 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Large"))
                .andExpect(jsonPath("$.priceDelta").value(2.00));
    }

    // 2. POST duplicate name → 409
    @Test
    void post_duplicateName_returns409() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.create(eq(bakeryId), eq(productId), any(CreateVariantRequest.class)))
                .thenThrow(new ConflictException("Variant 'Large' already exists for this product"));

        mockMvc.perform(post("/api/catalog/admin/products/" + productId + "/variants")
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateVariantRequest("Large", null, new BigDecimal("2.00"), 1))))
                .andExpect(status().isConflict());
    }

    // 3. POST as CUSTOMER → 403
    @Test
    void post_asCustomer_returns403() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal customer = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");

        mockMvc.perform(post("/api/catalog/admin/products/" + productId + "/variants")
                        .with(authentication(authToken(customer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateVariantRequest("Large", null, new BigDecimal("2.00"), 1))))
                .andExpect(status().isForbidden());
    }

    // 4. GET /api/catalog/admin/products/{productId}/variants → 200
    @Test
    void get_variantsForProduct_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.listForProduct(eq(bakeryId), eq(productId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new VariantResponse(UUID.randomUUID(), "Small", null, new BigDecimal("0.00"), 1),
                new VariantResponse(UUID.randomUUID(), "Large", "SKU-L", new BigDecimal("2.00"), 2)
        )));

        mockMvc.perform(get("/api/catalog/admin/products/" + productId + "/variants")
                        .with(authentication(authToken(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Small"))
                .andExpect(jsonPath("$.content[1].name").value("Large"));
    }

    // 5. PATCH /api/catalog/admin/variants/{variantId} → 200
    @Test
    void patch_existingVariant_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.update(eq(bakeryId), eq(variantId), any(UpdateVariantRequest.class)))
                .thenReturn(new VariantResponse(variantId, "Extra Large", null, new BigDecimal("3.50"), 1));

        mockMvc.perform(patch("/api/catalog/admin/variants/" + variantId)
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateVariantRequest("Extra Large", null, new BigDecimal("3.50"), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Extra Large"))
                .andExpect(jsonPath("$.priceDelta").value(3.50));
    }

    // 6. DELETE /api/catalog/admin/variants/{variantId} → 204
    @Test
    void delete_existingVariant_returns204() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        mockMvc.perform(delete("/api/catalog/admin/variants/" + variantId)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isNoContent());
    }
}
