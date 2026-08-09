USE `openapi`;
CREATE TABLE IF NOT EXISTS `notification` (
    `id`          BIGINT       NOT NULL COMMENT '主键（雪花）',
    `user_id`     BIGINT       NOT NULL COMMENT '接收人用户 ID',
    `type`        VARCHAR(32)  NOT NULL COMMENT '类型：SUBSCRIBE_APPLY / SUBSCRIBE_APPROVED / SUBSCRIBE_REJECTED',
    `title`       VARCHAR(128) NOT NULL COMMENT '标题',
    `content`     VARCHAR(512) DEFAULT NULL COMMENT '内容',
    `biz_id`      BIGINT       DEFAULT NULL COMMENT '关联业务 ID（订阅记录 ID）',
    `link`        VARCHAR(256) DEFAULT NULL COMMENT '跳转路由',
    `is_read`     TINYINT      DEFAULT 0 COMMENT '是否已读：0 未读 1 已读',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_delete`   TINYINT      DEFAULT 0 COMMENT '是否删除：0 否 1 是',
    PRIMARY KEY (`id`),
    KEY `idx_user_read_time` (`user_id`, `is_read`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内通知';
