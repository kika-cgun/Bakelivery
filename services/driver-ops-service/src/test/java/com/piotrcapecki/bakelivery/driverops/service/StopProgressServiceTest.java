package com.piotrcapecki.bakelivery.driverops.service;

import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import com.piotrcapecki.bakelivery.driverops.domain.StopProgress;
import com.piotrcapecki.bakelivery.driverops.domain.StopStatus;
import com.piotrcapecki.bakelivery.driverops.repository.DriverShiftRepository;
import com.piotrcapecki.bakelivery.driverops.repository.StopProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopProgressServiceTest {

    @Mock
    DriverShiftRepository shiftRepository;

    @Mock
    StopProgressRepository stopProgressRepository;

    @InjectMocks
    StopProgressService service;

    UUID driverId;
    UUID bakeryId;
    DriverShift activeShift;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();
        bakeryId = UUID.randomUUID();
        activeShift = DriverShift.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .driverId(driverId)
                .date(LocalDate.now())
                .routePlanId(UUID.randomUUID())
                .status(ShiftStatus.ACTIVE)
                .currentStopIndex(0)
                .startedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void completeStop_setsStatusAndAdvancesIndex() {
        UUID stopId = UUID.randomUUID();
        StopProgress stop = pendingStop(stopId, activeShift.getId(), bakeryId, 1);

        when(shiftRepository.findByDriverIdAndDateAndStatus(eq(driverId), any(), eq(ShiftStatus.ACTIVE)))
                .thenReturn(Optional.of(activeShift));
        when(stopProgressRepository.findByIdAndShiftId(stopId, activeShift.getId()))
                .thenReturn(Optional.of(stop));
        when(stopProgressRepository.save(any())).thenReturn(stop);
        when(shiftRepository.save(any())).thenReturn(activeShift);

        StopProgress result = service.completeStop(driverId, stopId);

        assertThat(result.getStatus()).isEqualTo(StopStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(shiftRepository).save(activeShift);
    }

    @Test
    void completeStop_throwsNotFoundForWrongShift() {
        when(shiftRepository.findByDriverIdAndDateAndStatus(eq(driverId), any(), eq(ShiftStatus.ACTIVE)))
                .thenReturn(Optional.of(activeShift));
        when(stopProgressRepository.findByIdAndShiftId(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeStop(driverId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void skipStop_movesStopToEndAndSetsSkipped() {
        UUID stopId = UUID.randomUUID();
        StopProgress stop = pendingStop(stopId, activeShift.getId(), bakeryId, 1);
        StopProgress stop2 = pendingStop(UUID.randomUUID(), activeShift.getId(), bakeryId, 2);
        StopProgress stop3 = pendingStop(UUID.randomUUID(), activeShift.getId(), bakeryId, 3);

        when(shiftRepository.findByDriverIdAndDateAndStatus(eq(driverId), any(), eq(ShiftStatus.ACTIVE)))
                .thenReturn(Optional.of(activeShift));
        when(stopProgressRepository.findByIdAndShiftId(stopId, activeShift.getId()))
                .thenReturn(Optional.of(stop));
        when(stopProgressRepository.findByShiftIdAndStatusOrderBySequenceNumberAsc(activeShift.getId(), StopStatus.PENDING))
                .thenReturn(List.of(stop, stop2, stop3));
        when(stopProgressRepository.save(any())).thenReturn(stop);
        when(shiftRepository.save(any())).thenReturn(activeShift);

        StopProgress result = service.skipStop(driverId, stopId, "Customer not home");

        assertThat(result.getStatus()).isEqualTo(StopStatus.SKIPPED);
        assertThat(result.getSkippedReason()).isEqualTo("Customer not home");
        assertThat(result.getSequenceNumber()).isEqualTo(4);
    }

    @Test
    void reorderStops_reassignsSequenceNumbers() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        StopProgress s1 = pendingStop(id1, activeShift.getId(), bakeryId, 1);
        StopProgress s2 = pendingStop(id2, activeShift.getId(), bakeryId, 2);
        StopProgress s3 = pendingStop(id3, activeShift.getId(), bakeryId, 3);

        when(shiftRepository.findByDriverIdAndDateAndStatus(eq(driverId), any(), eq(ShiftStatus.ACTIVE)))
                .thenReturn(Optional.of(activeShift));
        when(stopProgressRepository.findByShiftIdAndStatusOrderBySequenceNumberAsc(activeShift.getId(), StopStatus.PENDING))
                .thenReturn(List.of(s1, s2, s3));
        when(stopProgressRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<StopProgress> result = service.reorderStops(driverId, List.of(id3, id1, id2));

        assertThat(result.get(0).getId()).isEqualTo(id3);
        assertThat(result.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(result.get(1).getId()).isEqualTo(id1);
        assertThat(result.get(1).getSequenceNumber()).isEqualTo(2);
        assertThat(result.get(2).getId()).isEqualTo(id2);
        assertThat(result.get(2).getSequenceNumber()).isEqualTo(3);
    }

    private StopProgress pendingStop(UUID id, UUID shiftId, UUID bakeryId, int seq) {
        return StopProgress.builder()
                .id(id)
                .shiftId(shiftId)
                .bakeryId(bakeryId)
                .dispatchStopId(UUID.randomUUID())
                .routeStopId(UUID.randomUUID())
                .sequenceNumber(seq)
                .customerName("Customer " + seq)
                .deliveryAddress("Address " + seq)
                .lat(52.0)
                .lon(21.0)
                .status(StopStatus.PENDING)
                .build();
    }
}
