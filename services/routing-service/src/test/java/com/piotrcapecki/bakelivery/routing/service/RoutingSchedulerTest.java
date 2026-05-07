package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.config.RabbitConfig;
import com.piotrcapecki.bakelivery.routing.event.OptimizeRequest;
import com.piotrcapecki.bakelivery.routing.model.RoutePlan;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingSchedulerTest {

    @Mock
    RoutePlanRepository planRepo;

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    RoutingScheduler scheduler;

    @Test
    void nightlyOptimizeSendsOneMessagePerUniqueBakery() {
        UUID bakery1 = UUID.randomUUID();
        UUID bakery2 = UUID.randomUUID();
        UUID driver1 = UUID.randomUUID();
        UUID driver2 = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        RoutePlan plan1 = RoutePlan.builder()
                .bakeryId(bakery1).driverId(driver1).date(today)
                .status(RoutePlanStatus.PENDING).build();
        RoutePlan plan2 = RoutePlan.builder()
                .bakeryId(bakery1).driverId(driver2).date(today)
                .status(RoutePlanStatus.PENDING).build();
        RoutePlan plan3 = RoutePlan.builder()
                .bakeryId(bakery2).driverId(driver1).date(today)
                .status(RoutePlanStatus.PENDING).build();

        when(planRepo.findByDate(today)).thenReturn(List.of(plan1, plan2, plan3));

        scheduler.nightlyOptimize();

        ArgumentCaptor<OptimizeRequest> captor = ArgumentCaptor.forClass(OptimizeRequest.class);
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(RabbitConfig.ROUTING_EXCHANGE),
                eq(RabbitConfig.QUEUE_OPTIMIZE),
                captor.capture());

        List<OptimizeRequest> requests = captor.getAllValues();
        List<UUID> bakeryIds = requests.stream().map(OptimizeRequest::bakeryId).toList();

        assertThat(bakeryIds).containsExactlyInAnyOrder(bakery1, bakery2);
        assertThat(requests).allMatch(r -> r.date().equals(today))
                            .allMatch(r -> r.requestedBy().equals("nightly-cron"));
    }

    @Test
    void nightlyOptimizeHandlesEmptyPlansGracefully() {
        LocalDate today = LocalDate.now();
        when(planRepo.findByDate(today)).thenReturn(List.of());

        scheduler.nightlyOptimize();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
    }
}
