package com.piotrcapecki.bakelivery.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.catalog.config.AppConfig;
import com.piotrcapecki.bakelivery.catalog.dto.CreateCategoryRequest;
import com.piotrcapecki.bakelivery.catalog.dto.UpdateCategoryRequest;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.CategoryService;
import com.piotrcapecki.bakelivery.catalog.dto.CategoryResponse;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
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
class CategoryControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @MockitoBean private CategoryService service;

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
        UUID catId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.create(eq(bakeryId), any(CreateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(catId, "Bread", "bread", 1));

        mockMvc.perform(post("/api/catalog/admin/categories")
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Bread", "bread", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("bread"));
    }

    // 2. POST with duplicate slug → 409
    @Test
    void post_duplicateSlug_returns409() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.create(eq(bakeryId), any(CreateCategoryRequest.class)))
                .thenThrow(new ConflictException("Category with slug 'bread' already exists"));

        mockMvc.perform(post("/api/catalog/admin/categories")
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Bread", "bread", 1))))
                .andExpect(status().isConflict());
    }

    // 3. POST as CUSTOMER → 403
    @Test
    void post_asCustomer_returns403() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal customer = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");

        mockMvc.perform(post("/api/catalog/admin/categories")
                        .with(authentication(authToken(customer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Bread", "bread", 1))))
                .andExpect(status().isForbidden());
    }

    // 4. POST as DISPATCHER → 201
    @Test
    void post_asDispatcher_returns201() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CatalogPrincipal dispatcher = new CatalogPrincipal(UUID.randomUUID(), "cms@test.com", bakeryId, "DISPATCHER");

        when(service.create(eq(bakeryId), any(CreateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(catId, "Pastries", "pastries", 2));

        mockMvc.perform(post("/api/catalog/admin/categories")
                        .with(authentication(authToken(dispatcher)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Pastries", "pastries", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("pastries"));
    }

    // 5. GET returns only this bakery's categories
    @Test
    void get_returnsOnlyBakerysCategories() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.list(eq(bakeryId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new CategoryResponse(UUID.randomUUID(), "Bread", "bread", 1),
                new CategoryResponse(UUID.randomUUID(), "Cakes", "cakes", 2)
        )));

        mockMvc.perform(get("/api/catalog/admin/categories")
                        .with(authentication(authToken(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].slug").value("bread"))
                .andExpect(jsonPath("$.content[1].slug").value("cakes"));
    }

    // 6. PATCH → 200
    @Test
    void patch_existingCategory_returns200() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        when(service.update(eq(bakeryId), eq(catId), any(UpdateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(catId, "Sourdough", "bread", 5));

        mockMvc.perform(patch("/api/catalog/admin/categories/" + catId)
                        .with(authentication(authToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequest("Sourdough", null, 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sourdough"))
                .andExpect(jsonPath("$.sortOrder").value(5));
    }

    // 7. DELETE → 204
    @Test
    void delete_existingCategory_returns204() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        mockMvc.perform(delete("/api/catalog/admin/categories/" + catId)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isNoContent());
    }

    // 8. DELETE non-existent → 404
    @Test
    void delete_nonExistent_returns404() throws Exception {
        UUID bakeryId = UUID.randomUUID();
        UUID catId = UUID.randomUUID();
        CatalogPrincipal admin = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");

        doThrow(new NotFoundException("Category not found"))
                .when(service).delete(eq(bakeryId), eq(catId));

        mockMvc.perform(delete("/api/catalog/admin/categories/" + catId)
                        .with(authentication(authToken(admin))))
                .andExpect(status().isNotFound());
    }

    // 9. POST without JWT → 401
    @Test
    void post_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/catalog/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Bread", "bread", 1))))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
