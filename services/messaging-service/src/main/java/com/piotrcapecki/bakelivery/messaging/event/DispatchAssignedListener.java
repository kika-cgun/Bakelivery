package com.piotrcapecki.bakelivery.messaging.event;

import com.piotrcapecki.bakelivery.messaging.config.RabbitConfig;
import com.piotrcapecki.bakelivery.messaging.dto.DispatchAssignedEvent;
import com.piotrcapecki.bakelivery.messaging.service.ThreadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DispatchAssignedListener {

    private static final Logger logger = LoggerFactory.getLogger(DispatchAssignedListener.class);

    private final ThreadService threadService;

    @RabbitListener(queues = RabbitConfig.QUEUE_DISPATCH_ASSIGNED)
    public void onDispatchAssigned(DispatchAssignedEvent event) {
        logger.info("Received dispatch.assigned for orderId={} driverId={}", event.orderId(), event.driverId());
        threadService.assignDriver(event.bakeryId(), event.orderId(), event.driverId());
    }
}
