USE `openapi`;
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'invoke_log' AND COLUMN_NAME = 'request_headers');
SET @ddl := IF(@exist = 0,
    'ALTER TABLE `invoke_log` ADD COLUMN `request_headers` TEXT COMMENT ''请求头（JSON，脱敏后）'' AFTER `request_params`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
