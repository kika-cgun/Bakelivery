package com.piotrcapecki.bakelivery.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.auth.config.AppConfig;
import com.piotrcapecki.bakelivery.auth.dto.CreateEmployeeRequest;
import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.model.Role;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.repository.BakeryRepository;
import com.piotrcapecki.bakelivery.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
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
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBiYWtlbGl2ZXJ5");
        registry.add("jwt.access-ttl-millis", () -> "900000");
        registry.add("jwt.refresh-ttl-millis", () -> "2592000000");
    }

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BakeryRepository bakeryRepository;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();
        bakeryRepository.deleteAll();
    }

    @Test
    void bakeryAdminCannotSeeUsersFromOtherBakery() throws Exception {
        // Setup bakery A with admin + dispatcher
        Bakery bakeryA = bakeryRepository.saveAndFlush(Bakery.builder()
                .name("Bakery A")
                .slug("bakery-a-isolation-" + UUID.randomUUID())
                .build());
        User adminA = userRepository.saveAndFlush(User.builder()
                .email("admin-a@bakery-a.test")
                .passwordHash("hashedPw")
                .role(Role.BAKERY_ADMIN)
                .bakery(bakeryA)
                .build());
        User dispatcherA = userRepository.saveAndFlush(User.builder()
                .email("dispatcher-a@bakery-a.test")
                .passwordHash("hashedPw")
                .role(Role.DISPATCHER)
                .bakery(bakeryA)
                .build());

        // Setup bakery B with admin + dispatcher
        Bakery bakeryB = bakeryRepository.saveAndFlush(Bakery.builder()
                .name("Bakery B")
                .slug("bakery-b-isolation-" + UUID.randomUUID())
                .build());
        User adminB = userRepository.saveAndFlush(User.builder()
                .email("admin-b@bakery-b.test")
                .passwordHash("hashedPw")
                .role(Role.BAKERY_ADMIN)
                .bakery(bakeryB)
                .build());
        userRepository.saveAndFlush(User.builder()
                .email("dispatcher-b@bakery-b.test")
                .passwordHash("hashedPw")
                .role(Role.DISPATCHER)
                .bakery(bakeryB)
                .build());

        // Admin A calls GET /api/admin/users
        UsernamePasswordAuthenticationToken authA =
                new UsernamePasswordAuthenticationToken(adminA, null, adminA.getAuthorities());

        mockMvc.perform(get("/api/admin/users").with(authentication(authA)))
                .andExpect(status().isOk())
                // Only bakery A users appear
                .andExpect(jsonPath("$[?(@.email == 'admin-a@bakery-a.test')]").exists())
                .andExpect(jsonPath("$[?(@.email == 'dispatcher-a@bakery-a.test')]").exists())
                // Bakery B users do NOT appear
                .andExpect(jsonPath("$[?(@.email == 'admin-b@bakery-b.test')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.email == 'dispatcher-b@bakery-b.test')]").doesNotExist());
    }

    @Test
    void bakeryAdminCreatesEmployeeInOwnBakery_andDispatcherIsRejected() throws Exception {
        // Setup bakery A with admin and dispatcher
        Bakery bakeryA = bakeryRepository.saveAndFlush(Bakery.builder()
                .name("Bakery A Create")
                .slug("bakery-a-create-" + UUID.randomUUID())
                .build());
        User adminA = userRepository.saveAndFlush(User.builder()
                .email("admin-create@bakery-a.test")
                .passwordHash("hashedPw")
                .role(Role.BAKERY_ADMIN)
                .bakery(bakeryA)
                .build());
        User dispatcherA = userRepository.saveAndFlush(User.builder()
                .email("dispatcher-create@bakery-a.test")
                .passwordHash("hashedPw")
                .role(Role.DISPATCHER)
                .bakery(bakeryA)
                .build());

        // Admin A posts — created user must belong to bakery A (taken from JWT/principal, not request body)
        UsernamePasswordAuthenticationToken authA =
                new UsernamePasswordAuthenticationToken(adminA, null, adminA.getAuthorities());

        String requestBody = objectMapper.writeValueAsString(
                new CreateEmployeeRequest("new-employee@test.com", "temporaryPass123", Role.DRIVER));

        mockMvc.perform(post("/api/admin/users")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                // The created user is in bakery A (from JWT), not bakery B
                .andExpect(jsonPath("$.email").value("new-employee@test.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"));

        // Verify in DB: the newly created user belongs to bakery A
        User created = userRepository.findAllByBakeryId(bakeryA.getId()).stream()
                .filter(u -> u.getEmail().equals("new-employee@test.com"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Created user not found in bakery A"));

        // Dispatcher cannot call POST /api/admin/users (403)
        UsernamePasswordAuthenticationToken authDispatcher =
                new UsernamePasswordAuthenticationToken(dispatcherA, null, dispatcherA.getAuthorities());

        mockMvc.perform(post("/api/admin/users")
                        .with(authentication(authDispatcher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateEmployeeRequest("another@test.com", "temporaryPass123", Role.DRIVER))))
                .andExpect(status().isForbidden());
    }
}
