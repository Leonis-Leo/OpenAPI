USE `openapi`;
CREATE TABLE IF NOT EXISTS `audit_log` (
 `id` BIGINT NOT NULL, `user_id` BIGINT DEFAULT NULL, `action` VARCHAR(16) NOT NULL,
 `resource` VARCHAR(256) NOT NULL, `ip` VARCHAR(64) DEFAULT NULL, `status_code` INT DEFAULT NULL,
 `success` TINYINT DEFAULT 0, `detail` TEXT, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (`id`), KEY `idx_audit_user_time` (`user_id`, `create_time`), KEY `idx_audit_resource_time` (`resource`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员及用户操作审计日志';
