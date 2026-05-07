package com.piotrcapecki.bakelivery.dispatching.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.dispatching.dto.AssignDriverRequest;
import com.piotrcapecki.bakelivery.dispatching.dto.UpdateStatusRequest;
import com.piotrcapecki.bakelivery.dispatching.messaging.DispatchEventPublisher;
import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import com.piotrcapecki.bakelivery.dispatching.model.DispatchStopStatus;
import com.piotrcapecki.bakelivery.dispatching.repository.DispatchStopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchStopServiceTest {

    @Mock DispatchStopRepository repository;
    @Mock DispatchEventPublisher eventPublisher;
    @InjectMocks DispatchStopService service;

    @Test
    void assignDriver_updatesStopAndPublishesEvent() {
        UUID bakeryId = UUID.randomUUID();
        UUID stopId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        DispatchStop stop = DispatchStop.builder()
                .id(stopId).bakeryId(bakeryId).date(LocalDate.now())
                .customerName("X").deliveryAddress("Y").build();
        when(repository.findByIdAndBakeryId(stopId, bakeryId)).thenReturn(Optional.of(stop));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DispatchStop result = service.assignDriver(bakeryId, stopId,
                new AssignDriverRequest(driverId, "Jan Kierowca"));

        assertThat(result.getAssignedDriverId()).isEqualTo(driverId);
        assertThat(result.getAssignedDriverName()).isEqualTo("Jan Kierowca");
        assertThat(result.getStatus()).isEqualTo(DispatchStopStatus.ASSIGNED);
        verify(eventPublisher).publishAssigned(result);
    }

    @Test
    void assignDriver_throwsWhenWrongBakery() {
        UUID bakeryId = UUID.randomUUID();
        UUID stopId = UUID.randomUUID();
        when(repository.findByIdAndBakeryId(stopId, bakeryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignDriver(bakeryId, stopId,
                new AssignDriverRequest(UUID.randomUUID(), "X")))
                .isInstanceOf(NotFoundException.class);

        verify(eventPublisher, never()).publishAssigned(any());
    }

    @Test
    void updateStatus_setsNewStatus() {
        UUID bakeryId = UUID.randomUUID();
        UUID stopId = UUID.randomUUID();
        DispatchStop stop = DispatchStop.builder()
                .id(stopId).bakeryId(bakeryId).date(LocalDate.now())
                .customerName("X").deliveryAddress("Y").build();
        when(repository.findByIdAndBakeryId(stopId, bakeryId)).thenReturn(Optional.of(stop));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DispatchStop result = service.updateStatus(bakeryId, stopId, new UpdateStatusRequest("EN_ROUTE"));

        assertThat(result.getStatus()).isEqualTo(DispatchStopStatus.EN_ROUTE);
    }
}
