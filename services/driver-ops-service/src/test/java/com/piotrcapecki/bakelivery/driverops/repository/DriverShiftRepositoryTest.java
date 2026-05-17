package com.piotrcapecki.bakelivery.driverops.repository;

import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DriverShiftRepositoryTest {

    @Autowired
    DriverShiftRepository repository;

    @MockitoBean ConnectionFactory connectionFactory;
    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean S3Client s3Client;
    @MockitoBean S3Presigner s3Presigner;

    @Test
    void findByDriverIdAndDate_returnsShiftWhenExists() {
        UUID driverId = UUID.randomUUID();
        UUID bakeryId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        DriverShift shift = DriverShift.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .driverId(driverId)
                .date(today)
                .routePlanId(UUID.randomUUID())
                .status(ShiftStatus.ACTIVE)
                .currentStopIndex(0)
                .startedAt(OffsetDateTime.now())
                .build();
        repository.save(shift);

        Optional<DriverShift> found = repository.findByDriverIdAndDate(driverId, today);

        assertThat(found).isPresent();
        assertThat(found.get().getDriverId()).isEqualTo(driverId);
        assertThat(found.get().getStatus()).isEqualTo(ShiftStatus.ACTIVE);
    }

    @Test
    void findByBakeryIdAndDate_returnsAllShiftsForBakery() {
        UUID bakeryId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        DriverShift shift1 = DriverShift.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .driverId(UUID.randomUUID())
                .date(today)
                .routePlanId(UUID.randomUUID())
                .status(ShiftStatus.ACTIVE)
                .currentStopIndex(0)
                .startedAt(OffsetDateTime.now())
                .build();

        DriverShift shift2 = DriverShift.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .driverId(UUID.randomUUID())
                .date(today)
                .routePlanId(UUID.randomUUID())
                .status(ShiftStatus.ACTIVE)
                .currentStopIndex(0)
                .startedAt(OffsetDateTime.now())
                .build();

        repository.save(shift1);
        repository.save(shift2);

        assertThat(repository.findByBakeryIdAndDate(bakeryId, today)).hasSize(2);
    }
}
