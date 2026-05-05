package com.piotrcapecki.bakelivery.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrcapecki.bakelivery.customer.config.AppConfig;
import com.piotrcapecki.bakelivery.customer.dto.UpdateAddressRequest;
import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.model.CustomerType;
import com.piotrcapecki.bakelivery.customer.model.DeliveryAddress;
import com.piotrcapecki.bakelivery.customer.repository.CustomerRepository;
import com.piotrcapecki.bakelivery.customer.repository.DeliveryAddressRepository;
import com.piotrcapecki.bakelivery.customer.security.CustomerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBiYWtlbGl2ZXJ5");
        registry.add("jwt.access-ttl-millis", () -> "900000");
    }

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private DeliveryAddressRepository addressRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private UUID userIdA;
    private UUID bakeryIdA;
    private UUID userIdB;
    private UUID bakeryIdB;
    private Customer customerA;
    private Customer customerB;
    private DeliveryAddress addressA1;
    private DeliveryAddress addressA2;
    private DeliveryAddress addressB1;
    private DeliveryAddress addressB2;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        addressRepository.deleteAll();
        customerRepository.deleteAll();

        userIdA = UUID.randomUUID();
        bakeryIdA = UUID.randomUUID();
        userIdB = UUID.randomUUID();
        bakeryIdB = UUID.randomUUID();

        customerA = customerRepository.saveAndFlush(Customer.builder()
                .userId(userIdA).bakeryId(bakeryIdA).type(CustomerType.INDIVIDUAL)
                .firstName("Anna").lastName("A").build());
        customerB = customerRepository.saveAndFlush(Customer.builder()
                .userId(userIdB).bakeryId(bakeryIdB).type(CustomerType.INDIVIDUAL)
                .firstName("Bartek").lastName("B").build());

        addressA1 = addressRepository.saveAndFlush(DeliveryAddress.builder()
                .customerId(customerA.getId()).bakeryId(bakeryIdA)
                .label("Dom A1").street("ul. A 1").postalCode("00-001").city("Warszawa").build());
        addressA2 = addressRepository.saveAndFlush(DeliveryAddress.builder()
                .customerId(customerA.getId()).bakeryId(bakeryIdA)
                .label("Praca A2").street("ul. A 2").postalCode("00-002").city("Warszawa").build());
        addressB1 = addressRepository.saveAndFlush(DeliveryAddress.builder()
                .customerId(customerB.getId()).bakeryId(bakeryIdB)
                .label("Dom B1").street("ul. B 1").postalCode("11-001").city("Kraków").build());
        addressB2 = addressRepository.saveAndFlush(DeliveryAddress.builder()
                .customerId(customerB.getId()).bakeryId(bakeryIdB)
                .label("Praca B2").street("ul. B 2").postalCode("11-002").city("Kraków").build());
    }

    private UsernamePasswordAuthenticationToken authToken(UUID userId, UUID bakeryId, String email) {
        CustomerPrincipal principal = new CustomerPrincipal(userId, email, bakeryId, "CUSTOMER");
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void customerCannotReadProfileFromOtherBakery() throws Exception {
        // Confirm both profiles exist in DB — ensures isolation is meaningful, not just DB emptiness
        assertThat(customerRepository.findByUserIdAndBakeryId(userIdA, bakeryIdA)).isPresent();
        assertThat(customerRepository.findByUserIdAndBakeryId(userIdB, bakeryIdB)).isPresent();

        // A's token returns A's own profile (not B's), even though B's data is in the DB
        mockMvc.perform(get("/api/customer/profile").with(authentication(authToken(userIdA, bakeryIdA, "a@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.userId").value(userIdA.toString()));

        // A's userId combined with B's bakeryId (spoofed JWT) cannot access any profile
        mockMvc.perform(get("/api/customer/profile").with(authentication(authToken(userIdA, bakeryIdB, "a@test.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCannotReadAddressesFromOtherBakery() throws Exception {
        mockMvc.perform(get("/api/customer/addresses").with(authentication(authToken(userIdA, bakeryIdA, "a@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.street == 'ul. A 1')]").exists())
                .andExpect(jsonPath("$[?(@.street == 'ul. A 2')]").exists())
                .andExpect(jsonPath("$[?(@.street == 'ul. B 1')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.street == 'ul. B 2')]").doesNotExist());
    }

    @Test
    void customerCannotModifyAddressFromOtherBakery() throws Exception {
        String body = objectMapper.writeValueAsString(
                new UpdateAddressRequest(null, "ul. Hacked 99", "00-000", "Warszawa", null, null));

        mockMvc.perform(put("/api/customer/addresses/" + addressB1.getId())
                        .with(authentication(authToken(userIdA, bakeryIdA, "a@test.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        DeliveryAddress unchanged = addressRepository.findById(addressB1.getId()).orElseThrow();
        assertThat(unchanged.getStreet()).isEqualTo("ul. B 1");
    }

    @Test
    void customerCannotDeleteAddressFromOtherBakery() throws Exception {
        mockMvc.perform(delete("/api/customer/addresses/" + addressB1.getId())
                        .with(authentication(authToken(userIdA, bakeryIdA, "a@test.com"))))
                .andExpect(status().isNotFound());

        assertThat(addressRepository.findById(addressB1.getId())).isPresent();
    }
}
