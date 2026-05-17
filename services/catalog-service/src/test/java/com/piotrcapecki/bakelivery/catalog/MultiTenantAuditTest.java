package com.piotrcapecki.bakelivery.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.catalog.config.AppConfig;
import com.piotrcapecki.bakelivery.catalog.dto.CreateCategoryRequest;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class MultiTenantAuditTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private MinioStorageService minioStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private static final UUID BAKERY_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BAKERY_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken adminToken(UUID bakeryId) {
        CatalogPrincipal p = new CatalogPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_BAKERY_ADMIN")));
    }

    @Test
    void bakeryB_cannotSeeBakeryA_categories() throws Exception {
        // Bakery A creates a category
        mockMvc.perform(post("/api/catalog/admin/categories")
                        .with(authentication(adminToken(BAKERY_A_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCategoryRequest("Cakes", "cakes", 1))))
                .andExpect(status().isCreated());

        // Bakery B lists categories — should see 0 (isolation)
        mockMvc.perform(get("/api/catalog/admin/categories")
                        .with(authentication(adminToken(BAKERY_B_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void bakeryB_cannotAccessBakeryA_categoryById() throws Exception {
        // Bakery A creates a category
        MvcResult result = mockMvc.perform(post("/api/catalog/admin/categories")
                        .with(authentication(adminToken(BAKERY_A_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCategoryRequest("Pies", "pies", 2))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID categoryIdA = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        // Bakery B tries to update category owned by Bakery A → 404
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/catalog/admin/categories/" + categoryIdA)
                        .with(authentication(adminToken(BAKERY_B_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.piotrcapecki.bakelivery.catalog.dto.UpdateCategoryRequest("Hacked", "hacked", null))))
                .andExpect(status().isNotFound());
    }
}
