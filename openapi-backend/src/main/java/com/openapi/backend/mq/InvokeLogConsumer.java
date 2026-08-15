package com.openapi.backend.mq;

import com.openapi.common.trace.TraceContext;
import com.openapi.domain.mq.InvokeLogMessage;
import com.openapi.domain.mq.RabbitConstant;
import com.openapi.domain.entity.InvokeLog;
import com.openapi.domain.mapper.InvokeLogMapper;
import com.openapi.domain.mapper.InvokeStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;

/**
 * 调用日志消费者：异步写入 invoke_log，解耦统计逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvokeLogConsumer {

    private final InvokeLogMapper invokeLogMapper;
    private final InvokeStatsMapper statsMapper;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConstant.QUEUE_INVOKE_LOG)
    public void handle(InvokeLogMessage message,
                       @Header(value = TraceContext.HEADER_TRACE_ID, required = false) String traceId,
                       @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        if (isConsumed(messageId)) {
            log.info("duplicate invoke log message ignored, messageId={}", messageId);
            return;
        }
        TraceContext.put(traceId);
        log.info("invoke log consumed traceId={} appId={} interfaceId={}",
                traceId, message.getAppId(), message.getInterfaceId());
        try {
            transactionTemplate.executeWithoutResult(status -> {
                InvokeLog log = new InvokeLog();
                log.setInterfaceId(message.getInterfaceId());
                log.setAppId(message.getAppId());
                log.setUserId(message.getUserId());
                log.setIp(message.getIp());
                log.setMethod(message.getMethod());
                log.setPath(message.getPath());
                log.setRequestParams(message.getRequestParams());
                log.setRequestHeaders(message.getRequestHeaders());
                log.setResponseBody(message.getResponseBody());
                log.setStatusCode(message.getStatusCode());
                log.setSuccess(message.isSuccess() ? 1 : 0);
                log.setCostMs(message.getCostMs());
                invokeLogMapper.insert(log);

                int success = message.isSuccess() ? 1 : 0;
                LocalDate statDate = message.getCreateTime() == null
                        ? LocalDate.now()
                        : message.getCreateTime().toLocalDate();
                statsMapper.upsertDaily(statDate, message.getAppId(), message.getInterfaceId(),
                        success, 1 - success, message.getCostMs());
                statsMapper.upsertCounter(message.getAppId(), message.getInterfaceId(),
                        success, 1 - success, message.getCostMs());
            });
            markConsumed(messageId);
        } finally {
            TraceContext.clear();
        }
    }

    private boolean isConsumed(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(idempotencyKey(messageId)));
        } catch (Exception e) {
            log.warn("Redis unavailable, idempotency check skipped: {}", e.getMessage());
            return false;
        }
    }

    private void markConsumed(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().setIfAbsent(
                    idempotencyKey(messageId), "1", Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("Redis unavailable, idempotency mark skipped: {}", e.getMessage());
        }
    }

    private String idempotencyKey(String messageId) {
        return "openapi:mq:consumed:" + messageId;
    }
}
