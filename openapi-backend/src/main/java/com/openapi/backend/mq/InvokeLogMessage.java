package com.openapi.backend.mq;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 调用日志消息（生产者 -> MQ -> 消费者）。
 */
@Data
public class InvokeLogMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long interfaceId;

    private Long appId;

    private Long userId;

    private String ip;

    private String method;

    private String path;

    private String requestParams;

    private String responseBody;

    private Integer statusCode;

    private boolean success;

    private long costMs;
}
