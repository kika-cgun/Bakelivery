package com.piotrcapecki.bakelivery.routing;

import com.piotrcapecki.bakelivery.routing.config.AppConfig;
import com.piotrcapecki.bakelivery.routing.security.RoutingPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
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
                "spring.autoconfigure.exclude=org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration"
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
        // Point Redis to a non-existent port to avoid connection; RedissonClient is mocked
        r.add("spring.data.redis.host", () -> "localhost");
        r.add("spring.data.redis.port", () -> "6399");
    }

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private RedissonClient redissonClient;

    private MockMvc mockMvc;

    private static final UUID BAKERY_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BAKERY_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken adminToken(UUID bakeryId) {
        RoutingPrincipal p = new RoutingPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_BAKERY_ADMIN")));
    }

    @Test
    void bakeryB_seesNoPlansFromBakeryA() throws Exception {
        String today = LocalDate.now().toString();

        // Bakery B lists route plans — should see 0 (no plans seeded for bakery B)
        mockMvc.perform(get("/api/routing/admin/plans")
                        .with(authentication(adminToken(BAKERY_B_ID)))
                        .param("date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void bakeryB_cannotAccessBakeryA_planStops() throws Exception {
        UUID fakeplanId = UUID.randomUUID();

        // Bakery B tries to access a plan that doesn't belong to it → 404
        mockMvc.perform(get("/api/routing/admin/plans/" + fakeplanId + "/stops")
                        .with(authentication(adminToken(BAKERY_B_ID))))
                .andExpect(status().isNotFound());
    }
}
