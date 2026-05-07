package com.piotrcapecki.bakelivery.invoice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_INVOICE = "orders.invoice";

    @Bean
    public Queue ordersInvoiceQueue() {
        return QueueBuilder.durable(QUEUE_INVOICE)
                .withArgument("x-dead-letter-exchange", "bakelivery.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.invoice")
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
