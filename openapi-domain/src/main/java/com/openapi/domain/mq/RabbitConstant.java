package com.openapi.domain.mq;

/**
 * RabbitMQ 常量。
 */
public final class RabbitConstant {

    public static final String EXCHANGE_INVOKE = "openapi.invoke.exchange";
    public static final String QUEUE_INVOKE_LOG = "openapi.invoke.log";
    public static final String ROUTING_INVOKE_LOG = "openapi.invoke.log";
    public static final String EXCHANGE_INVOKE_DLX = "openapi.invoke.dlx";
    public static final String QUEUE_INVOKE_LOG_DLQ = "openapi.invoke.log.dlq";
    public static final String ROUTING_INVOKE_LOG_DLQ = "openapi.invoke.log.dlq";

    private RabbitConstant() {
    }
}
