package com.piotrcapecki.bakelivery.driverops.messaging;

import com.piotrcapecki.bakelivery.driverops.config.RabbitConfig;
import com.piotrcapecki.bakelivery.driverops.dto.DeliveryCompletedEvent;
import com.piotrcapecki.bakelivery.driverops.dto.DeliverySkippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCompleted(DeliveryCompletedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.DELIVERY_COMPLETED_KEY, event);
        log.info("Published delivery.completed for dispatchStopId={}", event.dispatchStopId());
    }

    public void publishSkipped(DeliverySkippedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.DELIVERY_SKIPPED_KEY, event);
        log.info("Published delivery.skipped for dispatchStopId={}", event.dispatchStopId());
    }
}
