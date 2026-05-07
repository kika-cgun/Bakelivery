package com.piotrcapecki.bakelivery.dispatching.repository;

import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class FixedDeliveryPointRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired FixedDeliveryPointRepository repository;

    @Test
    void saveAndFindByBakeryId_returnsActivePoints() {
        UUID bakeryId = UUID.randomUUID();
        repository.save(FixedDeliveryPoint.builder()
                .bakeryId(bakeryId)
                .name("Punkt A")
                .address("ul. Piekarska 1, Kraków")
                .deliveryDays((short) 31)
                .build());

        List<FixedDeliveryPoint> result = repository.findByBakeryIdAndActiveTrue(bakeryId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Punkt A");
        assertThat(result.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void findByBakeryIdAndActiveTrue_excludesInactive() {
        UUID bakeryId = UUID.randomUUID();
        repository.save(FixedDeliveryPoint.builder()
                .bakeryId(bakeryId)
                .name("Nieaktywny")
                .address("ul. Stara 5")
                .active(false)
                .build());

        assertThat(repository.findByBakeryIdAndActiveTrue(bakeryId)).isEmpty();
    }

    @Test
    void findByIdAndBakeryId_isolatesTenants() {
        UUID bakeryA = UUID.randomUUID();
        UUID bakeryB = UUID.randomUUID();
        FixedDeliveryPoint point = repository.save(FixedDeliveryPoint.builder()
                .bakeryId(bakeryA).name("P").address("A").build());

        assertThat(repository.findByIdAndBakeryId(point.getId(), bakeryA)).isPresent();
        assertThat(repository.findByIdAndBakeryId(point.getId(), bakeryB)).isEmpty();
    }
}
