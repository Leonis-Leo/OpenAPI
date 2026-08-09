USE `openapi`;
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'interface_group' AND COLUMN_NAME = 'parent_id');
SET @ddl := IF(@exist = 0,
    'ALTER TABLE `interface_group` ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT ''父分组 ID（NULL 为顶级分组）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
