package com.piotrcapecki.bakelivery.dispatching.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.dispatching.dto.CreateFixedPointRequest;
import com.piotrcapecki.bakelivery.dispatching.dto.UpdateFixedPointRequest;
import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;
import com.piotrcapecki.bakelivery.dispatching.repository.FixedDeliveryPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixedDeliveryPointServiceTest {

    @Mock FixedDeliveryPointRepository repository;
    @InjectMocks FixedDeliveryPointService service;

    @Test
    void create_savesWithCorrectBakeryId() {
        UUID bakeryId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(bakeryId, new CreateFixedPointRequest("Sklep A", "ul. Główna 1", 50.0, 20.0, null, null));

        ArgumentCaptor<FixedDeliveryPoint> cap = ArgumentCaptor.forClass(FixedDeliveryPoint.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getBakeryId()).isEqualTo(bakeryId);
        assertThat(cap.getValue().getDeliveryDays()).isEqualTo((short) 127);
    }

    @Test
    void list_returnsActivePoints() {
        UUID bakeryId = UUID.randomUUID();
        when(repository.findByBakeryIdAndActiveTrue(bakeryId))
                .thenReturn(List.of(FixedDeliveryPoint.builder().bakeryId(bakeryId).name("A").address("B").build()));

        assertThat(service.list(bakeryId)).hasSize(1);
    }

    @Test
    void update_throwsWhenNotFound() {
        UUID bakeryId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndBakeryId(id, bakeryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(bakeryId, id, new UpdateFixedPointRequest(null, null, null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivate_setsActiveFalse() {
        UUID bakeryId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        FixedDeliveryPoint point = FixedDeliveryPoint.builder()
                .id(id).bakeryId(bakeryId).name("X").address("Y").build();
        when(repository.findByIdAndBakeryId(id, bakeryId)).thenReturn(Optional.of(point));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate(bakeryId, id);

        assertThat(point.isActive()).isFalse();
    }
}
