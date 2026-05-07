package com.piotrcapecki.bakelivery.order.repository;

import com.piotrcapecki.bakelivery.order.model.Order;
import com.piotrcapecki.bakelivery.order.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired OrderRepository orderRepo;

    private Order buildOrder(UUID bakeryId, UUID customerId) {
        return Order.builder()
                .bakeryId(bakeryId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("25.00"))
                .deliveryAddressId(UUID.randomUUID())
                .deliveryAddress("ul. Testowa 1, Warszawa")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    @Test
    void findAllByCustomerIdAndBakeryId_returnsScopedOrders() {
        UUID bakery = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        UUID otherCustomer = UUID.randomUUID();

        orderRepo.save(buildOrder(bakery, customer));
        orderRepo.save(buildOrder(bakery, customer));
        orderRepo.save(buildOrder(bakery, otherCustomer));

        List<Order> results = orderRepo
                .findAllByCustomerIdAndBakeryIdOrderByCreatedAtDesc(customer, bakery, Pageable.unpaged())
                .getContent();
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(o -> o.getCustomerId().equals(customer));
    }

    @Test
    void defaultStatusIsPlaced() {
        UUID bakery = UUID.randomUUID();
        Order saved = orderRepo.save(buildOrder(bakery, UUID.randomUUID()));
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void idempotencyKeyIsUnique() {
        UUID bakery = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        Order first = buildOrder(bakery, UUID.randomUUID());
        first.setIdempotencyKey(key);
        orderRepo.save(first);

        Order second = buildOrder(bakery, UUID.randomUUID());
        second.setIdempotencyKey(key);
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> orderRepo.saveAndFlush(second));
    }
}
