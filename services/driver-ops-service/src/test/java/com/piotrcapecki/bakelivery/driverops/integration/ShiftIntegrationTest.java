package com.piotrcapecki.bakelivery.driverops.integration;

import com.piotrcapecki.bakelivery.driverops.client.RouteStopDto;
import com.piotrcapecki.bakelivery.driverops.client.RoutingClient;
import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import com.piotrcapecki.bakelivery.driverops.domain.StopStatus;
import com.piotrcapecki.bakelivery.driverops.dto.StartShiftRequest;
import com.piotrcapecki.bakelivery.driverops.repository.DriverShiftRepository;
import com.piotrcapecki.bakelivery.driverops.repository.StopProgressRepository;
import com.piotrcapecki.bakelivery.driverops.service.ShiftService;
import com.piotrcapecki.bakelivery.driverops.service.StopProgressService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
class ShiftIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bakelivery_driver_ops")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6399");
    }

    @Autowired
    ShiftService shiftService;

    @Autowired
    StopProgressService stopProgressService;

    @Autowired
    DriverShiftRepository shiftRepository;

    @Autowired
    StopProgressRepository stopProgressRepository;

    @MockitoBean
    RoutingClient routingClient;

    @Test
    void startShiftAndCompleteStop_persistsCorrectly() {
        UUID driverId = UUID.randomUUID();
        UUID bakeryId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        RouteStopDto stop1 = new RouteStopDto(UUID.randomUUID(), UUID.randomUUID(), 1,
                52.1, 21.1, "Alice", "Street 1", 0.9, 300, "PENDING");
        RouteStopDto stop2 = new RouteStopDto(UUID.randomUUID(), UUID.randomUUID(), 2,
                52.2, 21.2, "Bob", "Street 2", 0.8, 600, "PENDING");

        when(routingClient.getPlanStops(any(), anyString(), anyString()))
                .thenReturn(List.of(stop1, stop2));

        shiftService.startShift(driverId, bakeryId, new StartShiftRequest(planId), "Bearer test");

        DriverShift shift = shiftRepository.findByDriverIdAndDate(driverId, LocalDate.now()).orElseThrow();
        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.ACTIVE);
        assertThat(shift.getCurrentStopIndex()).isEqualTo(0);

        var stops = stopProgressRepository.findByShiftIdOrderBySequenceNumberAsc(shift.getId());
        assertThat(stops).hasSize(2);
        assertThat(stops.get(0).getStatus()).isEqualTo(StopStatus.PENDING);

        UUID firstStopId = stops.get(0).getId();
        stopProgressService.completeStop(driverId, firstStopId);

        var updatedStop = stopProgressRepository.findById(firstStopId).orElseThrow();
        assertThat(updatedStop.getStatus()).isEqualTo(StopStatus.COMPLETED);
        assertThat(updatedStop.getCompletedAt()).isNotNull();

        DriverShift updatedShift = shiftRepository.findByDriverIdAndDate(driverId, LocalDate.now()).orElseThrow();
        assertThat(updatedShift.getCurrentStopIndex()).isEqualTo(1);
    }
}
