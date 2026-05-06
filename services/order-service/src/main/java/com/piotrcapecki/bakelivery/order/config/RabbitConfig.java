package com.piotrcapecki.bakelivery.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE         = "bakelivery.events";
    public static final String DLX              = "bakelivery.dlx";
    public static final String QUEUE_NOTIFICATION = "orders.notification";
    public static final String QUEUE_INVOICE      = "orders.invoice";
    public static final String DLQ_NOTIFICATION   = "dlq.notification";
    public static final String DLQ_INVOICE        = "dlq.invoice";
    public static final String ROUTING_ORDER_PLACED = "order.placed";

    @Bean
    public TopicExchange bakeliveryEvents() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange dlx() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue ordersNotificationQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_NOTIFICATION)
                .build();
    }

    @Bean
    public Queue ordersInvoiceQueue() {
        return QueueBuilder.durable(QUEUE_INVOICE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_INVOICE)
                .build();
    }

    @Bean
    public Queue dlqNotification() {
        return QueueBuilder.durable(DLQ_NOTIFICATION).build();
    }

    @Bean
    public Queue dlqInvoice() {
        return QueueBuilder.durable(DLQ_INVOICE).build();
    }

    @Bean
    public Binding bindingNotification(Queue ordersNotificationQueue, TopicExchange bakeliveryEvents) {
        return BindingBuilder.bind(ordersNotificationQueue)
                .to(bakeliveryEvents)
                .with(ROUTING_ORDER_PLACED);
    }

    @Bean
    public Binding bindingInvoice(Queue ordersInvoiceQueue, TopicExchange bakeliveryEvents) {
        return BindingBuilder.bind(ordersInvoiceQueue)
                .to(bakeliveryEvents)
                .with(ROUTING_ORDER_PLACED);
    }

    @Bean
    public Binding bindingDlqNotification(Queue dlqNotification, DirectExchange dlx) {
        return BindingBuilder.bind(dlqNotification).to(dlx).with(DLQ_NOTIFICATION);
    }

    @Bean
    public Binding bindingDlqInvoice(Queue dlqInvoice, DirectExchange dlx) {
        return BindingBuilder.bind(dlqInvoice).to(dlx).with(DLQ_INVOICE);
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
