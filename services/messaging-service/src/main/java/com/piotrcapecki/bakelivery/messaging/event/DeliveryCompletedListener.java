package com.piotrcapecki.bakelivery.messaging.event;

import com.piotrcapecki.bakelivery.messaging.config.RabbitConfig;
import com.piotrcapecki.bakelivery.messaging.dto.DeliveryCompletedEvent;
import com.piotrcapecki.bakelivery.messaging.service.ThreadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryCompletedListener {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryCompletedListener.class);

    private final ThreadService threadService;

    @RabbitListener(queues = RabbitConfig.QUEUE_DELIVERY_COMPLETED)
    public void onDeliveryCompleted(DeliveryCompletedEvent event) {
        logger.info("Received delivery.completed for orderId={}, closing thread", event.orderId());
        threadService.closeThread(event.bakeryId(), event.orderId());
    }
}
