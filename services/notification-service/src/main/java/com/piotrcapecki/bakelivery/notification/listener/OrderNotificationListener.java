package com.piotrcapecki.bakelivery.notification.listener;

import com.piotrcapecki.bakelivery.notification.dto.OrderPlacedEvent;
import com.piotrcapecki.bakelivery.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final EmailService emailService;

    @RabbitListener(queues = "orders.notification")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed event for order {}", event.orderId());
        emailService.sendOrderConfirmation(event);
    }
}
