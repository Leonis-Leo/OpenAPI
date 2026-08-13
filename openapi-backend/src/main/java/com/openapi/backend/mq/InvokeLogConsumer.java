package com.openapi.backend.mq;

import com.openapi.domain.mq.InvokeLogMessage;
import com.openapi.domain.mq.RabbitConstant;
import com.openapi.domain.entity.InvokeLog;
import com.openapi.domain.mapper.InvokeLogMapper;
import com.openapi.domain.mapper.InvokeStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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

    @RabbitListener(queues = RabbitConstant.QUEUE_INVOKE_LOG)
    public void handle(InvokeLogMessage message) {
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
    }
}
