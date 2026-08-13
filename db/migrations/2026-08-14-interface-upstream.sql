USE `openapi`;

-- 接口上游服务配置：upstream / timeout_ms / retry_count（幂等，可重复执行）
SET @exist_upstream := (SELECT COUNT(*) FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'interface_info' AND COLUMN_NAME = 'upstream');
SET @ddl_upstream := IF(@exist_upstream = 0,
    'ALTER TABLE `interface_info` ADD COLUMN `upstream` VARCHAR(512) DEFAULT NULL COMMENT ''上游服务地址（配置后代理到上游）'' AFTER `response_example`',
    'SELECT 1');
PREPARE s FROM @ddl_upstream; EXECUTE s; DEALLOCATE PREPARE s;

SET @exist_timeout := (SELECT COUNT(*) FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'interface_info' AND COLUMN_NAME = 'timeout_ms');
SET @ddl_timeout := IF(@exist_timeout = 0,
    'ALTER TABLE `interface_info` ADD COLUMN `timeout_ms` INT DEFAULT 3000 COMMENT ''上游调用超时（毫秒）'' AFTER `upstream`',
    'SELECT 1');
PREPARE s FROM @ddl_timeout; EXECUTE s; DEALLOCATE PREPARE s;

SET @exist_retry := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'interface_info' AND COLUMN_NAME = 'retry_count');
SET @ddl_retry := IF(@exist_retry = 0,
    'ALTER TABLE `interface_info` ADD COLUMN `retry_count` INT DEFAULT 0 COMMENT ''失败重试次数'' AFTER `timeout_ms`',
    'SELECT 1');
PREPARE s FROM @ddl_retry; EXECUTE s; DEALLOCATE PREPARE s;
