package com.openapi.backend.mq;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

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

    private String requestHeaders;

    private String responseBody;

    private Integer statusCode;

    private boolean success;

    private long costMs;

    /** 调用发生时间（用于按天聚合，避免跨日偏差） */
    private LocalDateTime createTime;
}
