package com.piotrcapecki.bakelivery.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RabbitConfigTest {

    // In test profile RabbitAutoConfiguration is excluded, so we mock what's needed
    @MockitoBean RabbitTemplate rabbitTemplate;

    @Autowired TopicExchange bakeliveryEvents;
    @Autowired DirectExchange dlx;
    @Autowired Queue ordersNotificationQueue;
    @Autowired Queue ordersInvoiceQueue;
    @Autowired Queue dlqNotification;
    @Autowired Queue dlqInvoice;
    @Autowired Jackson2JsonMessageConverter messageConverter;

    @Test
    void exchangeNameIsCorrect() {
        assertThat(bakeliveryEvents.getName()).isEqualTo("bakelivery.events");
        assertThat(bakeliveryEvents.isDurable()).isTrue();
    }

    @Test
    void dlxNameIsCorrect() {
        assertThat(dlx.getName()).isEqualTo("bakelivery.dlx");
    }

    @Test
    void notificationQueueHasDlxArgument() {
        assertThat(ordersNotificationQueue.getName()).isEqualTo("orders.notification");
        assertThat(ordersNotificationQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", "bakelivery.dlx")
                .containsEntry("x-dead-letter-routing-key", "dlq.notification");
    }

    @Test
    void invoiceQueueHasDlxArgument() {
        assertThat(ordersInvoiceQueue.getName()).isEqualTo("orders.invoice");
        assertThat(ordersInvoiceQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", "bakelivery.dlx")
                .containsEntry("x-dead-letter-routing-key", "dlq.invoice");
    }

    @Test
    void dlqsAreDeclared() {
        assertThat(dlqNotification.getName()).isEqualTo("dlq.notification");
        assertThat(dlqInvoice.getName()).isEqualTo("dlq.invoice");
    }

    @Test
    void messageConverterIsJackson() {
        assertThat(messageConverter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
