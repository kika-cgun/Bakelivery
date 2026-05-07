package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.config.RabbitConfig;
import com.piotrcapecki.bakelivery.routing.event.DispatchAssignedEvent;
import com.piotrcapecki.bakelivery.routing.event.OptimizeRequest;
import com.piotrcapecki.bakelivery.routing.model.RoutePlanStatus;
import com.piotrcapecki.bakelivery.routing.repository.RoutePlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchAssignedListenerTest {

    @Mock
    RoutePlanRepository planRepo;

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    DispatchAssignedListener listener;

    @Test
    void handleSkipsOptimizeWhenPlanAlreadyExists() {
        UUID bakeryId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        UUID dispatchStopId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DispatchAssignedEvent event = new DispatchAssignedEvent(
                dispatchStopId, bakeryId, driverId, "Driver Name",
                date, "123 Main St", 40.7128, -74.0060, orderId);

        when(planRepo.existsByBakeryIdAndDateAndStatusIn(
                eq(bakeryId), eq(date), anyList())).thenReturn(true);

        listener.handle(event);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
    }

    @Test
    void handleSendsOptimizeMessageWhenNoPlanExists() {
        UUID bakeryId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        UUID dispatchStopId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DispatchAssignedEvent event = new DispatchAssignedEvent(
                dispatchStopId, bakeryId, driverId, "Driver Name",
                date, "123 Main St", 40.7128, -74.0060, orderId);

        when(planRepo.existsByBakeryIdAndDateAndStatusIn(
                eq(bakeryId), eq(date), anyList())).thenReturn(false);

        listener.handle(event);

        ArgumentCaptor<OptimizeRequest> captor = ArgumentCaptor.forClass(OptimizeRequest.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.ROUTING_EXCHANGE),
                eq(RabbitConfig.QUEUE_OPTIMIZE),
                captor.capture());

        OptimizeRequest request = captor.getValue();
        assertThat(request.bakeryId()).isEqualTo(bakeryId);
        assertThat(request.date()).isEqualTo(date);
        assertThat(request.requestedBy()).isEqualTo("auto");
    }

    @Test
    void handleChecksForReadyOptimizingAndInProgressStatuses() {
        UUID bakeryId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        UUID dispatchStopId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        DispatchAssignedEvent event = new DispatchAssignedEvent(
                dispatchStopId, bakeryId, driverId, "Driver Name",
                date, "123 Main St", 40.7128, -74.0060, orderId);

        when(planRepo.existsByBakeryIdAndDateAndStatusIn(
                eq(bakeryId), eq(date), anyList())).thenReturn(false);

        listener.handle(event);

        ArgumentCaptor<List> statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(planRepo).existsByBakeryIdAndDateAndStatusIn(
                eq(bakeryId), eq(date), statusCaptor.capture());

        List<RoutePlanStatus> statuses = statusCaptor.getValue();
        assertThat(statuses).containsExactlyInAnyOrder(
                RoutePlanStatus.READY,
                RoutePlanStatus.OPTIMIZING,
                RoutePlanStatus.IN_PROGRESS);
    }
}
