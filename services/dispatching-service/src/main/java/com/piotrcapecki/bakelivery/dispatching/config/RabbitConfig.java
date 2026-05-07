package com.piotrcapecki.bakelivery.dispatching.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "bakelivery.events";
    public static final String QUEUE_ORDER_PLACED = "dispatch.order_placed";
    public static final String ROUTING_ORDER_PLACED = "order.placed";
    public static final String DLX = "bakelivery.events.dlx";
    public static final String DLQ_ORDER_PLACED = "dispatch.order_placed.dlq";

    @Bean
    public TopicExchange bakeliveryExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_PLACED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ORDER_PLACED)
                .build();
    }

    @Bean
    public Queue orderPlacedDlq() {
        return QueueBuilder.durable(DLQ_ORDER_PLACED).build();
    }

    @Bean
    public Binding orderPlacedBinding(Queue orderPlacedQueue, TopicExchange bakeliveryExchange) {
        return BindingBuilder.bind(orderPlacedQueue).to(bakeliveryExchange).with(ROUTING_ORDER_PLACED);
    }

    @Bean
    public Binding dlqBinding(Queue orderPlacedDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(orderPlacedDlq).to(deadLetterExchange).with(DLQ_ORDER_PLACED);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
