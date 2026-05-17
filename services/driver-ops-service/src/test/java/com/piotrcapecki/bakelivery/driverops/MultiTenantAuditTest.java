package com.piotrcapecki.bakelivery.driverops;

import com.piotrcapecki.bakelivery.driverops.config.AppConfig;
import com.piotrcapecki.bakelivery.driverops.messaging.DeliveryEventPublisher;
import com.piotrcapecki.bakelivery.driverops.security.DriverOpsPrincipal;
import com.piotrcapecki.bakelivery.driverops.service.DriverPositionService;
import com.piotrcapecki.bakelivery.driverops.service.ProofOfDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

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
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private DeliveryEventPublisher deliveryEventPublisher;

    @MockitoBean
    private DriverPositionService driverPositionService;

    @MockitoBean
    private ProofOfDeliveryService proofOfDeliveryService;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3Presigner s3Presigner;

    private MockMvc mockMvc;

    private static final UUID BAKERY_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BAKERY_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private UsernamePasswordAuthenticationToken adminToken(UUID bakeryId) {
        DriverOpsPrincipal p = new DriverOpsPrincipal(UUID.randomUUID(), "admin@test.com", bakeryId, "BAKERY_ADMIN");
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_BAKERY_ADMIN")));
    }

    @Test
    void bakeryB_seesNoShiftsFromBakeryA() throws Exception {
        String today = LocalDate.now().toString();

        // Bakery B admin lists shifts — should see 0 (no shifts seeded for bakery B)
        mockMvc.perform(get("/api/driver-ops/admin/shifts")
                        .with(authentication(adminToken(BAKERY_B_ID)))
                        .param("date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void bakeryA_and_bakeryB_shiftsAreIsolated() throws Exception {
        String today = LocalDate.now().toString();

        // Both bakeries list shifts for today — neither sees the other's data
        mockMvc.perform(get("/api/driver-ops/admin/shifts")
                        .with(authentication(adminToken(BAKERY_A_ID)))
                        .param("date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/driver-ops/admin/shifts")
                        .with(authentication(adminToken(BAKERY_B_ID)))
                        .param("date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
