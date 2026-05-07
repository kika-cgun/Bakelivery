package com.piotrcapecki.bakelivery.routing.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EVENTS_EXCHANGE         = "bakelivery.events";
    public static final String ROUTING_EXCHANGE        = "bakelivery.routing.direct";
    public static final String DLX                     = "bakelivery.dlx";
    public static final String QUEUE_OPTIMIZE          = "routing.optimize";
    public static final String QUEUE_DISPATCH_ASSIGNED = "routing.dispatch_assigned";
    public static final String DLQ_OPTIMIZE            = "dlq.routing.optimize";
    public static final String DLQ_DISPATCH            = "dlq.routing.dispatch_assigned";

    @Bean
    TopicExchange bakeliveryEvents() {
        return ExchangeBuilder.topicExchange(EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange routingDirect() {
        return ExchangeBuilder.directExchange(ROUTING_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange dlx() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    Queue routingOptimizeQueue() {
        return QueueBuilder.durable(QUEUE_OPTIMIZE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_OPTIMIZE)
                .build();
    }

    @Bean
    Queue routingDispatchAssignedQueue() {
        return QueueBuilder.durable(QUEUE_DISPATCH_ASSIGNED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_DISPATCH)
                .build();
    }

    @Bean
    Queue dlqOptimize() { return QueueBuilder.durable(DLQ_OPTIMIZE).build(); }

    @Bean
    Queue dlqDispatch() { return QueueBuilder.durable(DLQ_DISPATCH).build(); }

    @Bean
    Binding optimizeBinding(Queue routingOptimizeQueue, DirectExchange routingDirect) {
        return BindingBuilder.bind(routingOptimizeQueue).to(routingDirect).with(QUEUE_OPTIMIZE);
    }

    @Bean
    Binding dispatchAssignedBinding(Queue routingDispatchAssignedQueue, TopicExchange bakeliveryEvents) {
        return BindingBuilder.bind(routingDispatchAssignedQueue).to(bakeliveryEvents).with("dispatch.assigned");
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        var t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        t.setMandatory(true);
        return t;
    }
}
