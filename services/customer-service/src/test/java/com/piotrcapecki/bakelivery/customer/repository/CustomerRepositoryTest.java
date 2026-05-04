package com.piotrcapecki.bakelivery.customer.repository;

import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.model.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class CustomerRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired CustomerRepository repository;

    @Test
    void saveAndFindByUserId_returnsSavedCustomer() {
        UUID userId = UUID.randomUUID();
        UUID bakeryId = UUID.randomUUID();
        Customer saved = repository.save(Customer.builder()
                .userId(userId)
                .bakeryId(bakeryId)
                .type(CustomerType.INDIVIDUAL)
                .firstName("Anna")
                .lastName("Kowalska")
                .phone("+48 600 000 000")
                .build());

        Optional<Customer> found = repository.findByUserId(userId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getBakeryId()).isEqualTo(bakeryId);
        assertThat(found.get().getFirstName()).isEqualTo("Anna");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void findByUserIdAndBakeryId_filtersByTenant() {
        UUID userId = UUID.randomUUID();
        UUID bakeryA = UUID.randomUUID();
        UUID bakeryB = UUID.randomUUID();
        repository.save(Customer.builder().userId(userId).bakeryId(bakeryA).type(CustomerType.INDIVIDUAL).build());

        assertThat(repository.findByUserIdAndBakeryId(userId, bakeryA)).isPresent();
        assertThat(repository.findByUserIdAndBakeryId(userId, bakeryB)).isEmpty();
    }

    @Test
    void uniqueUserIdConstraint_preventsTwoCustomersForOneUser() {
        UUID userId = UUID.randomUUID();
        repository.save(Customer.builder().userId(userId).bakeryId(UUID.randomUUID()).type(CustomerType.INDIVIDUAL).build());
        repository.flush();

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    repository.save(Customer.builder().userId(userId).bakeryId(UUID.randomUUID()).type(CustomerType.INDIVIDUAL).build());
                    repository.flush();
                }
        );
    }
}
