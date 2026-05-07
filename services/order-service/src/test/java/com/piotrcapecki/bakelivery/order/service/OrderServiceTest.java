package com.piotrcapecki.bakelivery.order.service;

import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.order.client.CatalogClient;
import com.piotrcapecki.bakelivery.order.dto.*;
import com.piotrcapecki.bakelivery.order.dto.catalog.MediaResponse;
import com.piotrcapecki.bakelivery.order.dto.catalog.ProductDetailResponse;
import com.piotrcapecki.bakelivery.order.dto.catalog.ProductResponse;
import com.piotrcapecki.bakelivery.order.dto.catalog.VariantResponse;
import com.piotrcapecki.bakelivery.order.model.OrderStatus;
import com.piotrcapecki.bakelivery.order.repository.OrderRepository;
import com.piotrcapecki.bakelivery.order.security.OrderPrincipal;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired OrderService service;
    @Autowired OrderRepository orderRepo;
    @MockitoBean CatalogClient catalogClient;
    @MockitoBean RabbitTemplate rabbitTemplate;

    private ProductDetailResponse activeProduct(UUID productId, BigDecimal price) {
        return new ProductDetailResponse(
                new ProductResponse(productId, null, "SKU-1", "bread", "Bread",
                        "Fresh", price, (short) 127, true),
                List.of(),
                List.of());
    }

    private ProductDetailResponse inactiveProduct(UUID productId) {
        return new ProductDetailResponse(
                new ProductResponse(productId, null, "SKU-X", "old", "Old",
                        "Stale", new BigDecimal("5.00"), (short) 127, false),
                List.of(),
                List.of());
    }

    @Test
    void placeOrder_success_publishesEventAndReturns201Data() {
        UUID bakery = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderPrincipal principal = new OrderPrincipal(customer, "cust@test.com", bakery, "CUSTOMER");

        when(catalogClient.getProduct(eq(productId), anyString(), anyString()))
                .thenReturn(activeProduct(productId, new BigDecimal("10.00")));

        CreateOrderRequest req = new CreateOrderRequest(
                UUID.randomUUID(), "ul. Testowa 1", List.of(new OrderItemRequest(productId, null, 2)), null);

        OrderResponse response = service.placeOrder(principal, req, UUID.randomUUID().toString(), "Bearer token");

        assertThat(response.status()).isEqualTo("PLACED");
        assertThat(response.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("20.00");
        verify(rabbitTemplate).convertAndSend(eq("bakelivery.events"), eq("order.placed"), any(OrderPlacedEvent.class));
    }

    @Test
    void placeOrder_inactiveProduct_throwsIllegalArgument() {
        UUID bakery = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderPrincipal principal = new OrderPrincipal(UUID.randomUUID(), "x@test.com", bakery, "CUSTOMER");

        when(catalogClient.getProduct(eq(productId), anyString(), anyString()))
                .thenReturn(inactiveProduct(productId));

        CreateOrderRequest req = new CreateOrderRequest(
                UUID.randomUUID(), "ul. A 1", List.of(new OrderItemRequest(productId, null, 1)), null);

        assertThatThrownBy(() -> service.placeOrder(principal, req, UUID.randomUUID().toString(), "Bearer t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void placeOrder_productNotFound_throwsNotFoundException() {
        UUID bakery = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderPrincipal principal = new OrderPrincipal(UUID.randomUUID(), "x@test.com", bakery, "CUSTOMER");

        when(catalogClient.getProduct(eq(productId), anyString(), anyString()))
                .thenThrow(FeignException.NotFound.class);

        CreateOrderRequest req = new CreateOrderRequest(
                UUID.randomUUID(), "ul. A 1", List.of(new OrderItemRequest(productId, null, 1)), null);

        assertThatThrownBy(() -> service.placeOrder(principal, req, UUID.randomUUID().toString(), "Bearer t"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void placeOrder_duplicateIdempotencyKey_throwsConflict() {
        UUID bakery = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderPrincipal principal = new OrderPrincipal(UUID.randomUUID(), "cust@test.com", bakery, "CUSTOMER");
        String key = UUID.randomUUID().toString();

        when(catalogClient.getProduct(eq(productId), anyString(), anyString()))
                .thenReturn(activeProduct(productId, new BigDecimal("5.00")));

        CreateOrderRequest req = new CreateOrderRequest(
                UUID.randomUUID(), "ul. A 1", List.of(new OrderItemRequest(productId, null, 1)), null);

        service.placeOrder(principal, req, key, "Bearer token");

        assertThatThrownBy(() -> service.placeOrder(principal, req, key, "Bearer token"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Duplicate order");
    }

    @Test
    void updateStatus_changesOrderStatus() {
        UUID bakery = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderPrincipal principal = new OrderPrincipal(UUID.randomUUID(), "cust@test.com", bakery, "CUSTOMER");

        when(catalogClient.getProduct(eq(productId), anyString(), anyString()))
                .thenReturn(activeProduct(productId, new BigDecimal("5.00")));

        CreateOrderRequest req = new CreateOrderRequest(
                UUID.randomUUID(), "ul. A 1", List.of(new OrderItemRequest(productId, null, 1)), null);

        OrderResponse placed = service.placeOrder(principal, req, UUID.randomUUID().toString(), "Bearer t");

        OrderResponse updated = service.updateStatus(placed.id(), bakery, new UpdateOrderStatusRequest("ACCEPTED"));
        assertThat(updated.status()).isEqualTo("ACCEPTED");
    }
}
