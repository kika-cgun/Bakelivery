package com.piotrcapecki.bakelivery.invoice.listener;

import com.piotrcapecki.bakelivery.invoice.dto.OrderPlacedEvent;
import com.piotrcapecki.bakelivery.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceGenerationListener {

    private final InvoiceService invoiceService;

    @RabbitListener(queues = "orders.invoice")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed event for invoice generation, orderId={}", event.orderId());
        invoiceService.processOrderPlaced(event);
    }
}
