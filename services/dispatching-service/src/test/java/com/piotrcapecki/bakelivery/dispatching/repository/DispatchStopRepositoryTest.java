package com.piotrcapecki.bakelivery.dispatching.repository;

import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import com.piotrcapecki.bakelivery.dispatching.model.DispatchStopStatus;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class DispatchStopRepositoryTest {

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

    @Autowired DispatchStopRepository repository;

    private DispatchStop buildStop(UUID bakeryId, LocalDate date) {
        return DispatchStop.builder()
                .bakeryId(bakeryId)
                .date(date)
                .customerName("Jan Nowak")
                .deliveryAddress("ul. Kwiatowa 3")
                .build();
    }

    @Test
    void findByBakeryIdAndDate_returnsStopsForDay() {
        UUID bakeryId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        repository.save(buildStop(bakeryId, today));
        repository.save(buildStop(bakeryId, today.plusDays(1)));

        List<DispatchStop> result = repository.findByBakeryIdAndDate(bakeryId, today);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(today);
    }

    @Test
    void existsByDateAndOrderId_detectsDuplicate() {
        UUID orderId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        repository.save(DispatchStop.builder()
                .bakeryId(UUID.randomUUID())
                .date(date)
                .orderId(orderId)
                .customerName("A")
                .deliveryAddress("B")
                .build());

        assertThat(repository.existsByDateAndOrderId(date, orderId)).isTrue();
        assertThat(repository.existsByDateAndOrderId(date, UUID.randomUUID())).isFalse();
    }

    @Test
    void findByIdAndBakeryId_isolatesTenants() {
        UUID bakeryA = UUID.randomUUID();
        UUID bakeryB = UUID.randomUUID();
        DispatchStop stop = repository.save(buildStop(bakeryA, LocalDate.now()));

        assertThat(repository.findByIdAndBakeryId(stop.getId(), bakeryA)).isPresent();
        assertThat(repository.findByIdAndBakeryId(stop.getId(), bakeryB)).isEmpty();
    }

    @Test
    void newStop_hasStatusPending() {
        DispatchStop stop = repository.save(buildStop(UUID.randomUUID(), LocalDate.now()));
        assertThat(stop.getStatus()).isEqualTo(DispatchStopStatus.PENDING);
    }
}
