package com.piotrcapecki.bakelivery.driverops.service;

import com.piotrcapecki.bakelivery.driverops.client.RouteStopDto;
import com.piotrcapecki.bakelivery.driverops.client.RoutingClient;
import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import com.piotrcapecki.bakelivery.driverops.domain.StopProgress;
import com.piotrcapecki.bakelivery.driverops.domain.StopStatus;
import com.piotrcapecki.bakelivery.driverops.dto.ShiftResponse;
import com.piotrcapecki.bakelivery.driverops.dto.ShiftWithStopsResponse;
import com.piotrcapecki.bakelivery.driverops.dto.StartShiftRequest;
import com.piotrcapecki.bakelivery.driverops.repository.DriverShiftRepository;
import com.piotrcapecki.bakelivery.driverops.repository.StopProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    DriverShiftRepository shiftRepository;

    @Mock
    StopProgressRepository stopProgressRepository;

    @Mock
    RoutingClient routingClient;

    @InjectMocks
    ShiftService shiftService;

    UUID driverId;
    UUID bakeryId;
    UUID planId;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();
        bakeryId = UUID.randomUUID();
        planId = UUID.randomUUID();
    }

    @Test
    void startShift_savesShiftAndStops() {
        when(shiftRepository.findByDriverIdAndDate(eq(driverId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        RouteStopDto stop1 = new RouteStopDto(UUID.randomUUID(), UUID.randomUUID(), 1,
                52.1, 21.1, "Alice", "Street 1", 0.9, 300, "PENDING");
        RouteStopDto stop2 = new RouteStopDto(UUID.randomUUID(), UUID.randomUUID(), 2,
                52.2, 21.2, "Bob", "Street 2", 0.8, 600, "PENDING");

        when(routingClient.getPlanStops(eq(planId), anyString(), anyString()))
                .thenReturn(List.of(stop1, stop2));

        DriverShift savedShift = DriverShift.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .driverId(driverId)
                .date(LocalDate.now())
                .routePlanId(planId)
                .status(ShiftStatus.ACTIVE)
                .currentStopIndex(0)
                .startedAt(OffsetDateTime.now())
                .build();
        when(shiftRepository.save(any(DriverShift.class))).thenReturn(savedShift);

        ShiftResponse response = shiftService.startShift(driverId, bakeryId,
                new StartShiftRequest(planId), "Bearer token");

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.currentStopIndex()).isEqualTo(0);

        ArgumentCaptor<List<StopProgress>> stopsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stopProgressRepository).saveAll(stopsCaptor.capture());
        assertThat(stopsCaptor.getValue()).hasSize(2);
        assertThat(stopsCaptor.getValue().get(0).getStatus()).isEqualTo(StopStatus.PENDING);
    }

    @Test
    void startShift_throwsConflictWhenShiftAlreadyExists() {
        DriverShift existing = DriverShift.builder()
                .id(UUID.randomUUID())
                .status(ShiftStatus.ACTIVE)
                .build();
        when(shiftRepository.findByDriverIdAndDate(eq(driverId), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> shiftService.startShift(driverId, bakeryId,
                new StartShiftRequest(planId), "Bearer token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already has a shift");
    }

    @Test
    void getCurrentShift_identifiesCurrentStop() {
        DriverShift shift = DriverShift.builder()
                .id(UUID.randomUUID())
                .bakeryId(bakeryId)
                .driverId(driverId)
                .date(LocalDate.now())
                .routePlanId(planId)
                .status(ShiftStatus.ACTIVE)
                .currentStopIndex(0)
                .startedAt(OffsetDateTime.now())
                .build();

        when(shiftRepository.findByDriverIdAndDateAndStatus(eq(driverId), any(), eq(ShiftStatus.ACTIVE)))
                .thenReturn(Optional.of(shift));

        StopProgress pending = StopProgress.builder()
                .id(UUID.randomUUID())
                .shiftId(shift.getId())
                .bakeryId(bakeryId)
                .dispatchStopId(UUID.randomUUID())
                .routeStopId(UUID.randomUUID())
                .sequenceNumber(1)
                .customerName("Alice")
                .deliveryAddress("Street 1")
                .lat(52.0)
                .lon(21.0)
                .status(StopStatus.PENDING)
                .build();

        when(stopProgressRepository.findByShiftIdOrderBySequenceNumberAsc(shift.getId()))
                .thenReturn(List.of(pending));

        ShiftWithStopsResponse result = shiftService.getCurrentShift(driverId);

        assertThat(result.currentStop()).isNotNull();
        assertThat(result.currentStop().customerName()).isEqualTo("Alice");
        assertThat(result.allStops()).hasSize(1);
    }
}
