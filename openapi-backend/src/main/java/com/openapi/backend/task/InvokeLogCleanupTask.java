package com.openapi.backend.task;

import com.openapi.backend.mapper.InvokeLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * invoke_log 定时清理：按保留天数删除过期明细日志。
 *
 * <p>聚合统计表（invoke_stats_daily / invoke_stats_counter）永久保留，不受清理影响。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvokeLogCleanupTask {

    private final InvokeLogMapper invokeLogMapper;

    @Value("${openapi.log.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${openapi.log.cleanup-cron:0 0 3 * * ?}")
    public void cleanupExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        try {
            int deleted = invokeLogMapper.deleteOlderThan(cutoff);
            if (deleted > 0) {
                log.info("invoke_log cleanup: deleted {} rows older than {} days", deleted, retentionDays);
            }
        } catch (Exception e) {
            log.warn("invoke_log cleanup failed", e);
        }
    }
}
