package com.piotrcapecki.bakelivery.order;

import com.piotrcapecki.bakelivery.order.client.CatalogClient;
import com.piotrcapecki.bakelivery.order.config.AppConfig;
import com.piotrcapecki.bakelivery.order.security.OrderPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
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
    private CatalogClient catalogClient;

    private MockMvc mockMvc;

    private static final UUID BAKERY_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BAKERY_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID USER_A_ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab");
    private static final UUID USER_B_ID   = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbc");

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken customerToken(UUID userId, UUID bakeryId) {
        OrderPrincipal p = new OrderPrincipal(userId, "customer@test.com", bakeryId, "CUSTOMER");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private UsernamePasswordAuthenticationToken adminToken(UUID bakeryId) {
        OrderPrincipal p = new OrderPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_BAKERY_ADMIN")));
    }

    @Test
    void bakeryB_customer_seesEmptyOrderList() throws Exception {
        // Bakery B customer lists their orders — should see 0 (no data seeded for them)
        mockMvc.perform(get("/api/orders")
                        .with(authentication(customerToken(USER_B_ID, BAKERY_B_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void bakeryB_admin_seesEmptyAdminOrderList() throws Exception {
        // Bakery B admin lists all orders — should see 0 (no data seeded for bakery B)
        mockMvc.perform(get("/api/orders/admin")
                        .with(authentication(adminToken(BAKERY_B_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void bakeryB_admin_cannotAccessBakeryA_order() throws Exception {
        // Bakery B admin tries to get a random order UUID — should be 404
        UUID fakeOrderId = UUID.randomUUID();
        mockMvc.perform(get("/api/orders/admin/" + fakeOrderId)
                        .with(authentication(adminToken(BAKERY_B_ID))))
                .andExpect(status().isNotFound());
    }
}
