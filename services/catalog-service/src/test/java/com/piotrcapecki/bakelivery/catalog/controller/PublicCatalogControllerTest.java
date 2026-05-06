package com.piotrcapecki.bakelivery.catalog.controller;

import com.piotrcapecki.bakelivery.catalog.config.AppConfig;
import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.*;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(AppConfig.class)
class PublicCatalogControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @MockitoBean private CategoryService categoryService;
    @MockitoBean private ProductService productService;
    @MockitoBean private ProductVariantService variantService;
    @MockitoBean private ProductMediaService mediaService;

    private MockMvc mockMvc;

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

    // 1. GET /api/catalog/products as CUSTOMER → 200, only active products from own bakery
    @Test
    void listActiveProducts_asCustomer_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal customer = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");
        UUID productId = UUID.randomUUID();

        when(productService.listActive(eq(bakeryId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new ProductResponse(productId, UUID.randomUUID(), "SKU-001", "rye-bread",
                        "Rye Bread", "Fresh rye", new BigDecimal("5.00"), (short) 127, true)
        )));

        mockMvc.perform(get("/api/catalog/products")
                        .with(authentication(authToken(customer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("rye-bread"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    // 2. GET /api/catalog/products/{id} returns ProductDetailResponse with variants and media
    @Test
    void getProductDetail_asCustomer_returnsDetail() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CatalogPrincipal customer = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");

        ProductResponse product = new ProductResponse(productId, categoryId, "SKU-001", "croissant",
                "Croissant", null, new BigDecimal("3.50"), (short) 62, true);
        when(productService.get(bakeryId, productId)).thenReturn(product);
        when(variantService.listForProduct(bakeryId, productId)).thenReturn(List.of(
                new VariantResponse(UUID.randomUUID(), "Small", null, new BigDecimal("0.00"), 1),
                new VariantResponse(UUID.randomUUID(), "Large", null, new BigDecimal("1.00"), 2)
        ));
        when(mediaService.list(bakeryId, productId)).thenReturn(List.of(
                new MediaResponse(UUID.randomUUID(), "http://minio/img.jpg", "image/jpeg", 2048, 0, true, Instant.now().plusSeconds(900))
        ));

        mockMvc.perform(get("/api/catalog/products/" + productId)
                        .with(authentication(authToken(customer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.slug").value("croissant"))
                .andExpect(jsonPath("$.variants.length()").value(2))
                .andExpect(jsonPath("$.media.length()").value(1))
                .andExpect(jsonPath("$.media[0].primary").value(true));
    }

    // 3. GET other bakery's product → 404
    @Test
    void getProductDetail_otherBakery_returns404() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CatalogPrincipal customer = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");

        when(productService.get(eq(bakeryId), eq(productId)))
                .thenThrow(new NotFoundException("Product not found"));

        mockMvc.perform(get("/api/catalog/products/" + productId)
                        .with(authentication(authToken(customer))))
                .andExpect(status().isNotFound());
    }

    // 4. GET /api/catalog/products unauthenticated → 403 (stateless, no entry point)
    @Test
    void listProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/catalog/products"))
                .andExpect(status().isForbidden());
    }

    // 5. GET /api/catalog/categories returns own bakery's categories
    @Test
    void listCategories_asCustomer_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal customer = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");

        when(categoryService.list(eq(bakeryId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new CategoryResponse(UUID.randomUUID(), "Bread", "bread", 1),
                new CategoryResponse(UUID.randomUUID(), "Cakes", "cakes", 2)
        )));

        mockMvc.perform(get("/api/catalog/categories")
                        .with(authentication(authToken(customer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].slug").value("bread"))
                .andExpect(jsonPath("$.content[1].slug").value("cakes"));
    }

    // 6. DRIVER role can also read products
    @Test
    void listActiveProducts_asDriver_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal driver = new CatalogPrincipal(UUID.randomUUID(), "driver@test.com", bakeryId, "DRIVER");

        when(productService.listActive(eq(bakeryId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/catalog/products")
                        .with(authentication(authToken(driver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // 7. BAKERY_ADMIN can access public endpoints too
    @Test
    void listActiveProducts_asBakeryAdmin_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(productService.listActive(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/catalog/products")
                        .with(authentication(authToken(admin))))
                .andExpect(status().isOk());
    }
}
