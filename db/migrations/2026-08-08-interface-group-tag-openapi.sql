USE `openapi`;

-- 接口分组
CREATE TABLE IF NOT EXISTS `interface_group` (
    `id`          BIGINT      NOT NULL COMMENT '主键（雪花）',
    `name`        VARCHAR(64) NOT NULL COMMENT '分组名称',
    `sort_order`  INT         DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_delete`   TINYINT     DEFAULT 0 COMMENT '是否删除：0 否 1 是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口分组';

-- 接口标签
CREATE TABLE IF NOT EXISTS `interface_tag` (
    `id`          BIGINT      NOT NULL COMMENT '主键（雪花）',
    `name`        VARCHAR(32) NOT NULL COMMENT '标签名称',
    `color`       VARCHAR(16) DEFAULT '#2563eb' COMMENT '标签颜色',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_delete`   TINYINT     DEFAULT 0 COMMENT '是否删除：0 否 1 是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口标签';

-- 接口-标签关联（多对多）
CREATE TABLE IF NOT EXISTS `interface_tag_relation` (
    `id`           BIGINT   NOT NULL COMMENT '主键（雪花）',
    `interface_id` BIGINT   NOT NULL COMMENT '接口 ID',
    `tag_id`       BIGINT   NOT NULL COMMENT '标签 ID',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interface_tag` (`interface_id`, `tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口-标签关联';

-- 接口表补充分组字段
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = 'openapi' AND TABLE_NAME = 'interface_info' AND COLUMN_NAME = 'group_id');
SET @ddl := IF(@exist = 0,
    'ALTER TABLE `interface_info` ADD COLUMN `group_id` BIGINT DEFAULT NULL COMMENT ''所属分组''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
