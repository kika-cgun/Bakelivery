package com.piotrcapecki.bakelivery.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.catalog.config.AppConfig;
import com.piotrcapecki.bakelivery.catalog.dto.CreateCategoryRequest;
import com.piotrcapecki.bakelivery.catalog.dto.CreateProductRequest;
import com.piotrcapecki.bakelivery.catalog.dto.UpdateProductRequest;
import com.piotrcapecki.bakelivery.catalog.repository.CategoryRepository;
import com.piotrcapecki.bakelivery.catalog.repository.MediaAssetRepository;
import com.piotrcapecki.bakelivery.catalog.repository.ProductRepository;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@Import(AppConfig.class)
@Testcontainers
class MultiTenantIsolationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private WebApplicationContext context;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @MockitoBean private MinioStorageService minioStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private UUID bakeryIdA;
    private UUID bakeryIdB;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mediaAssetRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        bakeryIdA = UUID.randomUUID();
        bakeryIdB = UUID.randomUUID();
    }

    private UsernamePasswordAuthenticationToken adminToken(UUID bakeryId) {
        CatalogPrincipal p = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_BAKERY_ADMIN")));
    }

    private UsernamePasswordAuthenticationToken customerToken(UUID bakeryId) {
        CatalogPrincipal p = new CatalogPrincipal(UUID.randomUUID(), "cust@test.com", bakeryId, "CUSTOMER");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void fullIsolation() throws Exception {
        // 1. Bakery A creates category "bread" and product "bread-rye"
        MvcResult catResult = mockMvc.perform(post("/api/catalog/admin/categories")
                        .with(authentication(adminToken(bakeryIdA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCategoryRequest("Bread", "bread", 1))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID categoryIdA = UUID.fromString(
                objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult prodResult = mockMvc.perform(post("/api/catalog/admin/products")
                        .with(authentication(adminToken(bakeryIdA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProductRequest(categoryIdA, null, "bread-rye",
                                        "Rye Bread", null, new BigDecimal("4.50"), (short) 127))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID productIdA = UUID.fromString(
                objectMapper.readTree(prodResult.getResponse().getContentAsString()).get("id").asText());

        // 2. Bakery B creates category "bread" with the same slug — allowed (isolated namespaces)
        mockMvc.perform(post("/api/catalog/admin/categories")
                        .with(authentication(adminToken(bakeryIdB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCategoryRequest("Bread", "bread", 1))))
                .andExpect(status().isCreated());

        // 3. Bakery B GET /api/catalog/products → 0 results (doesn't see A's products)
        mockMvc.perform(get("/api/catalog/products")
                        .with(authentication(adminToken(bakeryIdB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 4. Bakery B GET /api/catalog/admin/products/{idA} → 404
        mockMvc.perform(get("/api/catalog/admin/products/" + productIdA)
                        .with(authentication(adminToken(bakeryIdB))))
                .andExpect(status().isNotFound());

        // 5. Bakery B PATCH /api/catalog/admin/products/{idA} → 404
        mockMvc.perform(patch("/api/catalog/admin/products/" + productIdA)
                        .with(authentication(adminToken(bakeryIdB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProductRequest(null, null, null, "Hacked", null, null, null, null))))
                .andExpect(status().isNotFound());

        // 6. Bakery B uploads media to productIdA → 404
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[128]);
        mockMvc.perform(multipart("/api/catalog/admin/products/" + productIdA + "/media")
                        .file(file)
                        .with(authentication(adminToken(bakeryIdB))))
                .andExpect(status().isNotFound());

        // 7. CUSTOMER of bakery B GET /api/catalog/products → 0 results
        mockMvc.perform(get("/api/catalog/products")
                        .with(authentication(customerToken(bakeryIdB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
