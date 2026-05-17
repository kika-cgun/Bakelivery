package com.piotrcapecki.bakelivery.driverops.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "bakelivery.events";
    public static final String DLX = "bakelivery.dlx";

    public static final String ROUTE_UPDATED_QUEUE = "driver_ops.route_updated";
    public static final String ROUTE_UPDATED_KEY = "route.updated";

    public static final String ROUTE_UPDATED_DLQ = "driver_ops.route_updated.dlq";

    public static final String DELIVERY_COMPLETED_KEY = "delivery.completed";
    public static final String DELIVERY_SKIPPED_KEY = "delivery.skipped";

    @Bean
    public TopicExchange bakeliveryExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue routeUpdatedQueue() {
        return QueueBuilder.durable(ROUTE_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", ROUTE_UPDATED_DLQ)
                .quorum()
                .build();
    }

    @Bean
    public Queue routeUpdatedDlq() {
        return QueueBuilder.durable(ROUTE_UPDATED_DLQ).quorum().build();
    }

    @Bean
    public Binding routeUpdatedBinding() {
        return BindingBuilder.bind(routeUpdatedQueue())
                .to(bakeliveryExchange())
                .with(ROUTE_UPDATED_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
