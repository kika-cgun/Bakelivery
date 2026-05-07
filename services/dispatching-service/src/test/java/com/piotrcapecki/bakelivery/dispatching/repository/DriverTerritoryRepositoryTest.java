package com.piotrcapecki.bakelivery.dispatching.repository;

import com.piotrcapecki.bakelivery.dispatching.model.DriverTerritory;
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
class DriverTerritoryRepositoryTest {

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

    @Autowired DriverTerritoryRepository territoryRepository;
    @Autowired FixedDeliveryPointRepository pointRepository;

    private FixedDeliveryPoint savedPoint(UUID bakeryId) {
        return pointRepository.save(FixedDeliveryPoint.builder()
                .bakeryId(bakeryId).name("P").address("A").build());
    }

    @Test
    void findByBakeryIdAndDriverId_returnsAssignedTerritories() {
        UUID bakeryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        FixedDeliveryPoint point = savedPoint(bakeryId);
        territoryRepository.save(DriverTerritory.builder()
                .bakeryId(bakeryId).driverId(driverId).driverName("Jan Kowalski")
                .fixedPoint(point).build());

        List<DriverTerritory> result = territoryRepository.findByBakeryIdAndDriverId(bakeryId, driverId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDriverName()).isEqualTo("Jan Kowalski");
    }

    @Test
    void existsByBakeryIdAndDriverIdAndFixedPointId_detectsDuplicate() {
        UUID bakeryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        FixedDeliveryPoint point = savedPoint(bakeryId);
        territoryRepository.save(DriverTerritory.builder()
                .bakeryId(bakeryId).driverId(driverId).driverName("Jan")
                .fixedPoint(point).build());

        assertThat(territoryRepository.existsByBakeryIdAndDriverIdAndFixedPointId(
                bakeryId, driverId, point.getId())).isTrue();
        assertThat(territoryRepository.existsByBakeryIdAndDriverIdAndFixedPointId(
                bakeryId, UUID.randomUUID(), point.getId())).isFalse();
    }
}
