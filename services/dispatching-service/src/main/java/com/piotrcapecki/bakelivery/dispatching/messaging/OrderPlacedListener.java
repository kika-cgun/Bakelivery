package com.piotrcapecki.bakelivery.dispatching.messaging;

import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import com.piotrcapecki.bakelivery.dispatching.repository.DispatchStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.piotrcapecki.bakelivery.dispatching.config.RabbitConfig.QUEUE_ORDER_PLACED;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedListener {

    private final DispatchStopRepository dispatchStopRepository;

    @RabbitListener(queues = QUEUE_ORDER_PLACED)
    @Transactional
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed for orderId={} bakeryId={}", event.orderId(), event.bakeryId());

        LocalDate today = LocalDate.now();
        if (dispatchStopRepository.existsByDateAndOrderId(today, event.orderId())) {
            log.warn("DispatchStop already exists for orderId={} date={}, skipping", event.orderId(), today);
            return;
        }

        DispatchStop stop = DispatchStop.builder()
                .bakeryId(event.bakeryId())
                .date(today)
                .orderId(event.orderId())
                .customerName(event.customerName())
                .deliveryAddress(event.deliveryAddress())
                .lat(event.lat())
                .lon(event.lon())
                .build();

        dispatchStopRepository.save(stop);
        log.info("Created DispatchStop id={} for orderId={}", stop.getId(), event.orderId());
    }
}
