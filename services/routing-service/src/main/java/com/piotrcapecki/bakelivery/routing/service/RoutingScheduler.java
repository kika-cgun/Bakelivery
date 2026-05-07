package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.config.RabbitConfig;
import com.piotrcapecki.bakelivery.routing.event.OptimizeRequest;
import com.piotrcapecki.bakelivery.routing.repository.RoutePlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingScheduler {

    private final RoutePlanRepository planRepo;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(cron = "0 30 5 * * *")
    public void nightlyOptimize() {
        LocalDate today = LocalDate.now();
        log.info("Nightly route optimization triggered for {}", today);

        planRepo.findByDate(today).stream()
                .map(plan -> plan.getBakeryId())
                .collect(Collectors.toSet())
                .forEach(bakeryId -> {
                    log.info("Queuing nightly optimization for bakery {}", bakeryId);
                    rabbitTemplate.convertAndSend(RabbitConfig.ROUTING_EXCHANGE, RabbitConfig.QUEUE_OPTIMIZE,
                            new OptimizeRequest(bakeryId, today, "nightly-cron"));
                });
    }
}
