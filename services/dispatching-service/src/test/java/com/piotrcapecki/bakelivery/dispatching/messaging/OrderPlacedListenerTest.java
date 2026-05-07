package com.piotrcapecki.bakelivery.dispatching.messaging;

import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import com.piotrcapecki.bakelivery.dispatching.repository.DispatchStopRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderPlacedListenerTest {

    @MockitoBean DispatchStopRepository dispatchStopRepository;
    @Autowired OrderPlacedListener listener;

    @Test
    void onOrderPlaced_createsDispatchStop() {
        UUID orderId = UUID.randomUUID();
        UUID bakeryId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId, bakeryId, UUID.randomUUID(),
                "Anna Kowalska", "ul. Kwiatowa 1",
                50.06, 19.94, BigDecimal.valueOf(120.00),
                LocalDateTime.now());
        when(dispatchStopRepository.existsByDateAndOrderId(any(LocalDate.class), eq(orderId))).thenReturn(false);
        when(dispatchStopRepository.save(any(DispatchStop.class))).thenAnswer(inv -> inv.getArgument(0));

        listener.onOrderPlaced(event);

        ArgumentCaptor<DispatchStop> cap = ArgumentCaptor.forClass(DispatchStop.class);
        verify(dispatchStopRepository).save(cap.capture());
        DispatchStop saved = cap.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getBakeryId()).isEqualTo(bakeryId);
        assertThat(saved.getCustomerName()).isEqualTo("Anna Kowalska");
        assertThat(saved.getDeliveryAddress()).isEqualTo("ul. Kwiatowa 1");
    }

    @Test
    void onOrderPlaced_skipsIfAlreadyExists() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId, UUID.randomUUID(), UUID.randomUUID(),
                "X", "Y", null, null, BigDecimal.ONE, LocalDateTime.now());
        when(dispatchStopRepository.existsByDateAndOrderId(any(LocalDate.class), eq(orderId))).thenReturn(true);

        listener.onOrderPlaced(event);

        verify(dispatchStopRepository, never()).save(any());
    }
}
