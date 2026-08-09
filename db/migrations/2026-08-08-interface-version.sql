USE `openapi`;
CREATE TABLE IF NOT EXISTS `interface_version` (
    `id`              BIGINT       NOT NULL COMMENT '主键（雪花）',
    `interface_id`    BIGINT       NOT NULL COMMENT '接口 ID',
    `version_no`      INT          NOT NULL COMMENT '版本号（从 1 递增）',
    `name`            VARCHAR(64)  NOT NULL COMMENT '接口名称快照',
    `description`     VARCHAR(512) DEFAULT NULL COMMENT '描述快照',
    `method`          VARCHAR(8)   NOT NULL COMMENT '请求方式快照',
    `url`             VARCHAR(256) NOT NULL COMMENT '路径快照',
    `request_params`  TEXT COMMENT '请求参数快照',
    `response_example` TEXT COMMENT '响应示例快照',
    `status`          TINYINT      DEFAULT 0 COMMENT '接口状态快照：0 下线 1 上线',
    `change_note`     VARCHAR(256) DEFAULT NULL COMMENT '变更说明',
    `create_by`       BIGINT       DEFAULT NULL COMMENT '操作人用户 ID',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_delete`       TINYINT      DEFAULT 0 COMMENT '是否删除：0 否 1 是',
    PRIMARY KEY (`id`),
    KEY `idx_interface_version` (`interface_id`, `version_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口发布版本快照';
