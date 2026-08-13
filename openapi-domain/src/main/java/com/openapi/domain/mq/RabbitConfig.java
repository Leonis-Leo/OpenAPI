package com.openapi.domain.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange invokeExchange() {
        return new DirectExchange(RabbitConstant.EXCHANGE_INVOKE, true, false);
    }

    @Bean
    public Queue invokeLogQueue() {
        return new Queue(RabbitConstant.QUEUE_INVOKE_LOG, true);
    }

    @Bean
    public Binding invokeLogBinding() {
        return BindingBuilder.bind(invokeLogQueue())
                .to(invokeExchange())
                .with(RabbitConstant.ROUTING_INVOKE_LOG);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
