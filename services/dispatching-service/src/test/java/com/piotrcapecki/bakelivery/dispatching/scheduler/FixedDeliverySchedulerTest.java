package com.piotrcapecki.bakelivery.dispatching.scheduler;

import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;
import com.piotrcapecki.bakelivery.dispatching.repository.DispatchStopRepository;
import com.piotrcapecki.bakelivery.dispatching.repository.FixedDeliveryPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixedDeliverySchedulerTest {

    @Mock FixedDeliveryPointRepository pointRepository;
    @Mock DispatchStopRepository stopRepository;
    @Mock Clock clock;
    @InjectMocks FixedDeliveryScheduler scheduler;

    // 2026-05-04 is a Monday (bit 0)
    private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);

    private void fixClock(LocalDate date) {
        when(clock.instant()).thenReturn(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void generateDailyStops_createsStopWhenBitSet() {
        fixClock(MONDAY);
        UUID bakeryId = UUID.randomUUID();
        UUID pointId = UUID.randomUUID();
        // deliveryDays=1 means bit 0 (Monday) only
        FixedDeliveryPoint point = FixedDeliveryPoint.builder()
                .id(pointId).bakeryId(bakeryId)
                .name("Sklep A").address("ul. Główna 1")
                .deliveryDays((short) 1).active(true).build();
        when(pointRepository.findByActiveTrue()).thenReturn(List.of(point));
        when(stopRepository.findByBakeryIdAndDate(bakeryId, MONDAY)).thenReturn(List.of());
        when(stopRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.generateDailyStops();

        ArgumentCaptor<DispatchStop> cap = ArgumentCaptor.forClass(DispatchStop.class);
        verify(stopRepository).save(cap.capture());
        DispatchStop saved = cap.getValue();
        assertThat(saved.getDate()).isEqualTo(MONDAY);
        assertThat(saved.getFixedPointId()).isEqualTo(pointId);
        assertThat(saved.getCustomerName()).isEqualTo("Sklep A");
    }

    @Test
    void generateDailyStops_skipsWhenBitNotSet() {
        // Tuesday (bit 1), but deliveryDays=1 (only Monday)
        LocalDate tuesday = MONDAY.plusDays(1);
        fixClock(tuesday);
        UUID bakeryId = UUID.randomUUID();
        FixedDeliveryPoint point = FixedDeliveryPoint.builder()
                .id(UUID.randomUUID()).bakeryId(bakeryId)
                .name("P").address("A")
                .deliveryDays((short) 1).active(true).build();
        when(pointRepository.findByActiveTrue()).thenReturn(List.of(point));

        scheduler.generateDailyStops();

        verify(stopRepository, never()).save(any());
    }

    @Test
    void generateDailyStops_skipsWhenStopAlreadyExists() {
        fixClock(MONDAY);
        UUID bakeryId = UUID.randomUUID();
        UUID pointId = UUID.randomUUID();
        FixedDeliveryPoint point = FixedDeliveryPoint.builder()
                .id(pointId).bakeryId(bakeryId)
                .name("P").address("A")
                .deliveryDays((short) 127).active(true).build();
        DispatchStop existing = DispatchStop.builder()
                .bakeryId(bakeryId).date(MONDAY).fixedPointId(pointId)
                .customerName("P").deliveryAddress("A").build();
        when(pointRepository.findByActiveTrue()).thenReturn(List.of(point));
        when(stopRepository.findByBakeryIdAndDate(bakeryId, MONDAY)).thenReturn(List.of(existing));

        scheduler.generateDailyStops();

        verify(stopRepository, never()).save(any());
    }

    @Test
    void bitMask_allDaysSet_127_isCorrect() {
        short allDays = 127;
        for (int bit = 0; bit <= 6; bit++) {
            assertThat((allDays >> bit) & 1).isEqualTo(1);
        }
    }
}
