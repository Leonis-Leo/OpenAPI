USE `openapi`;

-- invoke_log 补索引：API 日志列表按 应用/用户/时间范围 过滤查询加速（幂等，可重复执行）
SET @exist_app := (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'invoke_log' AND INDEX_NAME = 'idx_app_time');
SET @ddl_app := IF(@exist_app = 0, 'ALTER TABLE `invoke_log` ADD INDEX `idx_app_time` (`app_id`, `create_time`)', 'SELECT 1');
PREPARE s FROM @ddl_app; EXECUTE s; DEALLOCATE PREPARE s;

SET @exist_user := (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'invoke_log' AND INDEX_NAME = 'idx_user_time');
SET @ddl_user := IF(@exist_user = 0, 'ALTER TABLE `invoke_log` ADD INDEX `idx_user_time` (`user_id`, `create_time`)', 'SELECT 1');
PREPARE s FROM @ddl_user; EXECUTE s; DEALLOCATE PREPARE s;

SET @exist_time := (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'invoke_log' AND INDEX_NAME = 'idx_create_time');
SET @ddl_time := IF(@exist_time = 0, 'ALTER TABLE `invoke_log` ADD INDEX `idx_create_time` (`create_time`)', 'SELECT 1');
PREPARE s FROM @ddl_time; EXECUTE s; DEALLOCATE PREPARE s;
