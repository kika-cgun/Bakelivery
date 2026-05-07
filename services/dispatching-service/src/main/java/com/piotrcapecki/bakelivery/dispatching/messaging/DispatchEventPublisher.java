package com.piotrcapecki.bakelivery.dispatching.messaging;

import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.piotrcapecki.bakelivery.dispatching.config.RabbitConfig.EXCHANGE;

@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchEventPublisher {

    private static final String ROUTING_KEY = "dispatch.assigned";

    private final RabbitTemplate rabbitTemplate;

    public void publishAssigned(DispatchStop stop) {
        DispatchAssignedEvent event = new DispatchAssignedEvent(
                stop.getId(),
                stop.getBakeryId(),
                stop.getAssignedDriverId(),
                stop.getAssignedDriverName(),
                stop.getDate(),
                stop.getDeliveryAddress(),
                stop.getLat(),
                stop.getLon(),
                stop.getOrderId());
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
        log.info("Published dispatch.assigned for stopId={} driverId={}",
                stop.getId(), stop.getAssignedDriverId());
    }
}
