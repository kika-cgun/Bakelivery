package com.piotrcapecki.bakelivery.dispatching.service;

import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.dispatching.dto.CreateTerritoryRequest;
import com.piotrcapecki.bakelivery.dispatching.model.DriverTerritory;
import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;
import com.piotrcapecki.bakelivery.dispatching.repository.DriverTerritoryRepository;
import com.piotrcapecki.bakelivery.dispatching.repository.FixedDeliveryPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverTerritoryServiceTest {

    @Mock DriverTerritoryRepository territoryRepository;
    @Mock FixedDeliveryPointRepository pointRepository;
    @InjectMocks DriverTerritoryService service;

    @Test
    void create_throwsWhenPointNotBelongsToBakery() {
        UUID bakeryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID pointId = UUID.randomUUID();
        when(pointRepository.findByIdAndBakeryId(pointId, bakeryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(bakeryId,
                new CreateTerritoryRequest(driverId, "Jan", pointId)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_throwsOnDuplicate() {
        UUID bakeryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID pointId = UUID.randomUUID();
        FixedDeliveryPoint point = FixedDeliveryPoint.builder()
                .id(pointId).bakeryId(bakeryId).name("P").address("A").build();
        when(pointRepository.findByIdAndBakeryId(pointId, bakeryId)).thenReturn(Optional.of(point));
        when(territoryRepository.existsByBakeryIdAndDriverIdAndFixedPointId(bakeryId, driverId, pointId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(bakeryId,
                new CreateTerritoryRequest(driverId, "Jan", pointId)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_savesWhenValid() {
        UUID bakeryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID pointId = UUID.randomUUID();
        FixedDeliveryPoint point = FixedDeliveryPoint.builder()
                .id(pointId).bakeryId(bakeryId).name("P").address("A").build();
        when(pointRepository.findByIdAndBakeryId(pointId, bakeryId)).thenReturn(Optional.of(point));
        when(territoryRepository.existsByBakeryIdAndDriverIdAndFixedPointId(bakeryId, driverId, pointId))
                .thenReturn(false);
        when(territoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DriverTerritory result = service.create(bakeryId, new CreateTerritoryRequest(driverId, "Jan Kowalski", pointId));

        verify(territoryRepository).save(any(DriverTerritory.class));
        org.assertj.core.api.Assertions.assertThat(result.getDriverId()).isEqualTo(driverId);
    }

    @Test
    void delete_throwsWhenNotFound() {
        UUID bakeryId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(territoryRepository.findByIdAndBakeryId(id, bakeryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(bakeryId, id))
                .isInstanceOf(NotFoundException.class);
    }
}
