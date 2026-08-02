package com.openapi.backend.config;

/**
 * RabbitMQ 常量。
 */
public final class RabbitConstant {

    public static final String EXCHANGE_INVOKE = "openapi.invoke.exchange";
    public static final String QUEUE_INVOKE_LOG = "openapi.invoke.log";
    public static final String ROUTING_INVOKE_LOG = "openapi.invoke.log";

    private RabbitConstant() {
    }
}
