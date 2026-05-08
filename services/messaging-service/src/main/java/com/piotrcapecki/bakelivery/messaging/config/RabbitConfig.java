package com.piotrcapecki.bakelivery.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "bakelivery.events";
    public static final String DLX = "bakelivery.dlx";

    // Queues consumed by messaging-service
    public static final String QUEUE_DISPATCH_ASSIGNED = "messaging.dispatch_assigned";
    public static final String QUEUE_DELIVERY_COMPLETED = "messaging.delivery_completed";

    // Routing keys consumed
    public static final String RK_DISPATCH_ASSIGNED = "dispatch.assigned";
    public static final String RK_DELIVERY_COMPLETED = "delivery.completed";

    // Routing key produced
    public static final String RK_MESSAGE_CREATED = "message.created";

    @Bean
    public TopicExchange bakeliveryExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue dispatchAssignedQueue() {
        return QueueBuilder.durable(QUEUE_DISPATCH_ASSIGNED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_DISPATCH_ASSIGNED + ".dlq")
                .build();
    }

    @Bean
    public Queue deliveryCompletedQueue() {
        return QueueBuilder.durable(QUEUE_DELIVERY_COMPLETED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_DELIVERY_COMPLETED + ".dlq")
                .build();
    }

    @Bean
    public Queue dispatchAssignedDlq() {
        return QueueBuilder.durable(QUEUE_DISPATCH_ASSIGNED + ".dlq").build();
    }

    @Bean
    public Queue deliveryCompletedDlq() {
        return QueueBuilder.durable(QUEUE_DELIVERY_COMPLETED + ".dlq").build();
    }

    @Bean
    public Binding dispatchAssignedBinding(Queue dispatchAssignedQueue, TopicExchange bakeliveryExchange) {
        return BindingBuilder.bind(dispatchAssignedQueue).to(bakeliveryExchange).with(RK_DISPATCH_ASSIGNED);
    }

    @Bean
    public Binding deliveryCompletedBinding(Queue deliveryCompletedQueue, TopicExchange bakeliveryExchange) {
        return BindingBuilder.bind(deliveryCompletedQueue).to(bakeliveryExchange).with(RK_DELIVERY_COMPLETED);
    }

    @Bean
    public Binding dispatchAssignedDlqBinding(Queue dispatchAssignedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(dispatchAssignedDlq).to(deadLetterExchange)
                .with(QUEUE_DISPATCH_ASSIGNED + ".dlq");
    }

    @Bean
    public Binding deliveryCompletedDlqBinding(Queue deliveryCompletedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deliveryCompletedDlq).to(deadLetterExchange)
                .with(QUEUE_DELIVERY_COMPLETED + ".dlq");
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
