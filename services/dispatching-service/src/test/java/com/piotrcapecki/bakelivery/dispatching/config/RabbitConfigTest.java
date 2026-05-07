package com.piotrcapecki.bakelivery.dispatching.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RabbitConfigTest {

    @Autowired TopicExchange bakeliveryExchange;
    @Autowired Queue orderPlacedQueue;
    @Autowired Queue orderPlacedDlq;
    @Autowired Jackson2JsonMessageConverter messageConverter;
    @Autowired RabbitTemplate rabbitTemplate;

    @Test
    void exchangeIsNamedCorrectly() {
        assertThat(bakeliveryExchange.getName()).isEqualTo(RabbitConfig.EXCHANGE);
    }

    @Test
    void queueIsDurableWithDlx() {
        assertThat(orderPlacedQueue.isDurable()).isTrue();
        assertThat(orderPlacedQueue.getArguments()).containsKey("x-dead-letter-exchange");
    }

    @Test
    void dlqIsDurable() {
        assertThat(orderPlacedDlq.getName()).isEqualTo(RabbitConfig.DLQ_ORDER_PLACED);
        assertThat(orderPlacedDlq.isDurable()).isTrue();
    }

    @Test
    void rabbitTemplateUsesJsonConverter() {
        assertThat(rabbitTemplate.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
