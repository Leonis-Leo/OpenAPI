CREATE DATABASE IF NOT EXISTS `openapi`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `openapi`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user`
(
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `user_account`  VARCHAR(64)  NOT NULL COMMENT '账号',
    `user_password` VARCHAR(128) NOT NULL COMMENT '密码',
    `user_name`     VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `user_role`     VARCHAR(16)  DEFAULT 'user' COMMENT '角色：user/admin',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT      DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account` (`user_account`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 应用表（一个应用对应一对 AccessKey/SecretKey）
CREATE TABLE IF NOT EXISTS `app`
(
    `id`          BIGINT       NOT NULL COMMENT '主键（雪花）',
    `app_name`    VARCHAR(64)  NOT NULL COMMENT '应用名称',
    `access_key`  VARCHAR(64)  NOT NULL COMMENT 'AccessKey',
    `secret_key`  VARCHAR(128) NOT NULL COMMENT 'SecretKey',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户',
    `status`      TINYINT      DEFAULT 0 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT      DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_access_key` (`access_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用表';

-- 接口信息表（平台对外发布的接口）
CREATE TABLE IF NOT EXISTS `interface_info`
(
    `id`               BIGINT       NOT NULL COMMENT '主键（雪花）',
    `name`             VARCHAR(64)  NOT NULL COMMENT '接口名称',
    `description`      VARCHAR(512) DEFAULT NULL COMMENT '接口描述',
    `method`           VARCHAR(8)   NOT NULL COMMENT '请求方式：GET/POST',
    `url`              VARCHAR(256) NOT NULL COMMENT '接口路径',
    `request_params`   TEXT COMMENT '请求参数说明（JSON）',
    `response_example` TEXT COMMENT '响应示例（JSON）',
    `group_id`         BIGINT       DEFAULT NULL COMMENT '所属分组',
    `status`           TINYINT      DEFAULT 0 COMMENT '状态：0下线 1上线',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`        TINYINT      DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口信息表';

-- 接口分组
CREATE TABLE IF NOT EXISTS `interface_group` (
    `id`          BIGINT      NOT NULL COMMENT '主键（雪花）',
    `name`        VARCHAR(64) NOT NULL COMMENT '分组名称',
    `parent_id`   BIGINT      DEFAULT NULL COMMENT '父分组 ID（NULL 为顶级分组）',
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

-- 接口订阅表（开发者申请订阅接口，管理员审批后才有调用权限）
CREATE TABLE IF NOT EXISTS `interface_subscribe`
(
    `id`           BIGINT      NOT NULL COMMENT '主键（雪花）',
    `interface_id` BIGINT      NOT NULL COMMENT '接口 ID',
    `app_id`       BIGINT      NOT NULL COMMENT '应用 ID',
    `user_id`      BIGINT      NOT NULL COMMENT '申请用户 ID',
    `status`       TINYINT     DEFAULT 0 COMMENT '状态：0待审批 1已通过 2已拒绝',
    `create_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    TINYINT     DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interface_app` (`interface_id`, `app_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口订阅表';

-- 接口调用日志表（MQ 消费者异步写入，用于调用统计）
CREATE TABLE IF NOT EXISTS `invoke_log`
(
    `id`           BIGINT   NOT NULL COMMENT '主键（雪花）',
    `interface_id` BIGINT   NOT NULL COMMENT '接口 ID',
    `app_id`       BIGINT   NOT NULL COMMENT '应用 ID',
    `user_id`      BIGINT   NOT NULL COMMENT '用户 ID',
    `ip`           VARCHAR(64)  DEFAULT NULL COMMENT '调用方 IP',
    `method`       VARCHAR(8)   DEFAULT NULL COMMENT '请求方式',
    `path`         VARCHAR(256) DEFAULT NULL COMMENT '请求路径',
    `request_params` TEXT COMMENT '请求参数（JSON）',
    `request_headers` TEXT COMMENT '请求头（JSON，脱敏后）',
    `response_body`  TEXT COMMENT '响应体（JSON，截断）',
    `status_code`  INT      DEFAULT 0 COMMENT 'HTTP 状态码',
    `success`      TINYINT  DEFAULT 0 COMMENT '是否成功：0失败 1成功',
    `cost_ms`      BIGINT   DEFAULT 0 COMMENT '调用耗时（毫秒）',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_interface_time` (`interface_id`, `create_time`),
    KEY `idx_app_time` (`app_id`, `create_time`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口调用日志表';

-- 调用统计：按天聚合（计费/报表主表，永久保留，不随明细日志删除）
CREATE TABLE IF NOT EXISTS `invoke_stats_daily` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `stat_date`     DATE     NOT NULL COMMENT '统计日期',
    `app_id`        BIGINT   NOT NULL COMMENT '应用 ID',
    `interface_id`  BIGINT   NOT NULL COMMENT '接口 ID',
    `total`         BIGINT   DEFAULT 0 COMMENT '总调用',
    `success`       BIGINT   DEFAULT 0 COMMENT '成功数',
    `fail`          BIGINT   DEFAULT 0 COMMENT '失败数',
    `total_cost_ms` BIGINT   DEFAULT 0 COMMENT '累计耗时（毫秒）',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_app_interface` (`stat_date`, `app_id`, `interface_id`),
    KEY `idx_date` (`stat_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='调用统计-按天聚合';

-- 调用统计：累计计数器（概览/排行直接读取，零聚合计算）
CREATE TABLE IF NOT EXISTS `invoke_stats_counter` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id`        BIGINT   NOT NULL COMMENT '应用 ID',
    `interface_id`  BIGINT   NOT NULL COMMENT '接口 ID',
    `total`         BIGINT   DEFAULT 0 COMMENT '总调用',
    `success`       BIGINT   DEFAULT 0 COMMENT '成功数',
    `fail`          BIGINT   DEFAULT 0 COMMENT '失败数',
    `total_cost_ms` BIGINT   DEFAULT 0 COMMENT '累计耗时（毫秒）',
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_interface` (`app_id`, `interface_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='调用统计-累计计数';

-- 接口限流配置表（管理平台配置，网关按接口限流）
CREATE TABLE IF NOT EXISTS `rate_limit_config`
(
    `id`           BIGINT  NOT NULL COMMENT '主键（雪花）',
    `interface_id` BIGINT  NOT NULL COMMENT '接口 ID',
    `capacity`     INT     DEFAULT 20 COMMENT '令牌桶容量',
    `refill_rate`  INT     DEFAULT 5 COMMENT '每秒补充令牌数',
    `enabled`      TINYINT DEFAULT 1 COMMENT '是否启用：0否 1是',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interface` (`interface_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口限流配置表';

-- 应用限流配置表（按应用维度限流）
CREATE TABLE IF NOT EXISTS `app_rate_limit_config`
(
    `id`           BIGINT  NOT NULL COMMENT '主键（雪花）',
    `app_id`       BIGINT  NOT NULL COMMENT '应用 ID',
    `capacity`     INT     DEFAULT 20 COMMENT '令牌桶容量',
    `refill_rate`  INT     DEFAULT 5 COMMENT '每秒补充令牌数',
    `enabled`      TINYINT DEFAULT 1 COMMENT '是否启用：0否 1是',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app` (`app_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='应用限流配置表';

-- 演示数据
-- admin 的 BCrypt 哈希（cost 10），明文密码仍为 admin
INSERT INTO `user` (`id`, `user_account`, `user_password`, `user_name`, `user_role`)
VALUES (1, 'admin', '$2b$10$4SPZdBW1mq9oyas7qVL62ukMG9JB8tJ6fBlwdn6h11UfI0mgfG6SK', '管理员', 'admin');

INSERT INTO `app` (`id`, `app_name`, `access_key`, `secret_key`, `user_id`, `status`)
VALUES (1, '演示应用', 'demo-access-key', 'demo-secret-key', 1, 1);

INSERT INTO `interface_info` (`id`, `name`, `description`, `method`, `url`, `request_params`, `response_example`, `status`)
VALUES (1, '随机名称', '随机返回一个英文名', 'GET', '/api/demo/name', '{"prefix":"名称前缀(可选)"}',
        '{"code":0,"data":"Alice"}', 1),
       (2, '参数回显', '原样返回表单参数', 'POST', '/api/demo/echo', '{"任意参数":"原样回显"}',
        '{"code":0,"data":{}}', 1);

-- 演示应用默认已订阅两个演示接口（审批通过），保证 CI 测试可直接调用
INSERT INTO `interface_subscribe` (`id`, `interface_id`, `app_id`, `user_id`, `status`)
VALUES (1, 1, 1, 1, 1),
       (2, 2, 1, 1, 1);

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL, `user_id` BIGINT DEFAULT NULL, `action` VARCHAR(16) NOT NULL,
    `resource` VARCHAR(256) NOT NULL, `ip` VARCHAR(64) DEFAULT NULL, `status_code` INT DEFAULT NULL,
    `success` TINYINT DEFAULT 0, `detail` TEXT, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_audit_user_time` (`user_id`, `create_time`), KEY `idx_audit_resource_time` (`resource`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员及用户操作审计日志';

-- 站内通知表（订阅申请 / 审批结果）
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

-- 接口发布版本快照（发布/更新生成，支持变更 diff 与一键回滚）
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
