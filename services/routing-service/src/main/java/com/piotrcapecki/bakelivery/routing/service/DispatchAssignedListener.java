package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.config.RabbitConfig;
import com.piotrcapecki.bakelivery.routing.event.DispatchAssignedEvent;
import com.piotrcapecki.bakelivery.routing.event.OptimizeRequest;
import com.piotrcapecki.bakelivery.routing.model.RoutePlanStatus;
import com.piotrcapecki.bakelivery.routing.repository.RoutePlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.piotrcapecki.bakelivery.routing.config.RabbitConfig.QUEUE_DISPATCH_ASSIGNED;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchAssignedListener {

    private final RoutePlanRepository planRepo;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = QUEUE_DISPATCH_ASSIGNED)
    public void handle(DispatchAssignedEvent event) {
        boolean alreadyOptimizing = planRepo.existsByBakeryIdAndDateAndStatusIn(
                event.bakeryId(), event.date(),
                List.of(RoutePlanStatus.READY, RoutePlanStatus.OPTIMIZING, RoutePlanStatus.IN_PROGRESS));
        if (alreadyOptimizing) {
            log.debug("Plan already exists for bakery {} on {}, skipping auto-trigger",
                    event.bakeryId(), event.date());
            return;
        }
        log.info("First assignment for bakery {} on {}, triggering optimization", event.bakeryId(), event.date());
        rabbitTemplate.convertAndSend(RabbitConfig.ROUTING_EXCHANGE, RabbitConfig.QUEUE_OPTIMIZE,
                new OptimizeRequest(event.bakeryId(), event.date(), "auto"));
    }
}
