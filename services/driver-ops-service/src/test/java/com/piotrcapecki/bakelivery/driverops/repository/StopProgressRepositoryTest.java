package com.piotrcapecki.bakelivery.driverops.repository;

import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import com.piotrcapecki.bakelivery.driverops.domain.StopProgress;
import com.piotrcapecki.bakelivery.driverops.domain.StopStatus;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StopProgressRepositoryTest {

    @Autowired
    StopProgressRepository stopProgressRepository;

    @Autowired
    DriverShiftRepository shiftRepository;

    @MockitoBean ConnectionFactory connectionFactory;
    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean S3Client s3Client;
    @MockitoBean S3Presigner s3Presigner;

    private UUID shiftId;
    private UUID bakeryId;

    @BeforeEach
    void setUp() {
        bakeryId = UUID.randomUUID();
        shiftId = UUID.randomUUID();

        DriverShift shift = DriverShift.builder()
                .id(shiftId)
                .bakeryId(bakeryId)
                .driverId(UUID.randomUUID())
                .date(LocalDate.now())
                .routePlanId(UUID.randomUUID())
                .status(ShiftStatus.ACTIVE)
                .currentStopIndex(0)
                .startedAt(OffsetDateTime.now())
                .build();
        shiftRepository.save(shift);
    }

    @Test
    void findByShiftIdOrderBySequenceNumberAsc_returnsInOrder() {
        stopProgressRepository.save(buildStop(shiftId, bakeryId, 3));
        stopProgressRepository.save(buildStop(shiftId, bakeryId, 1));
        stopProgressRepository.save(buildStop(shiftId, bakeryId, 2));

        List<StopProgress> stops = stopProgressRepository.findByShiftIdOrderBySequenceNumberAsc(shiftId);

        assertThat(stops).hasSize(3);
        assertThat(stops.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(stops.get(1).getSequenceNumber()).isEqualTo(2);
        assertThat(stops.get(2).getSequenceNumber()).isEqualTo(3);
    }

    @Test
    void findByIdAndShiftId_returnsEmptyForWrongShift() {
        StopProgress stop = buildStop(shiftId, bakeryId, 1);
        stopProgressRepository.save(stop);

        Optional<StopProgress> result = stopProgressRepository.findByIdAndShiftId(stop.getId(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    private StopProgress buildStop(UUID shiftId, UUID bakeryId, int seq) {
        return StopProgress.builder()
                .id(UUID.randomUUID())
                .shiftId(shiftId)
                .bakeryId(bakeryId)
                .dispatchStopId(UUID.randomUUID())
                .routeStopId(UUID.randomUUID())
                .sequenceNumber(seq)
                .customerName("Customer " + seq)
                .deliveryAddress("Street " + seq)
                .lat(52.0 + seq)
                .lon(21.0 + seq)
                .status(StopStatus.PENDING)
                .build();
    }
}
