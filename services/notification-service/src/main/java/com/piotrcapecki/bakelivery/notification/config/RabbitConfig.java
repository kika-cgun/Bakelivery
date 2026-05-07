package com.piotrcapecki.bakelivery.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_NOTIFICATION = "orders.notification";

    @Bean
    public Queue ordersNotificationQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION)
                .withArgument("x-dead-letter-exchange", "bakelivery.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.notification")
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
