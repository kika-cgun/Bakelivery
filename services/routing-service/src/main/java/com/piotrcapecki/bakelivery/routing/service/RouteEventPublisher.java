package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.config.RabbitConfig;
import com.piotrcapecki.bakelivery.routing.event.RouteUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishRouteUpdated(RouteUpdatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, "route.updated", event);
    }
}
