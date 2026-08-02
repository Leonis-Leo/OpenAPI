CREATE DATABASE IF NOT EXISTS `openapi`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `openapi`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
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
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
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
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`             VARCHAR(64)  NOT NULL COMMENT '接口名称',
    `description`      VARCHAR(512) DEFAULT NULL COMMENT '接口描述',
    `method`           VARCHAR(8)   NOT NULL COMMENT '请求方式：GET/POST',
    `url`              VARCHAR(256) NOT NULL COMMENT '接口路径',
    `request_params`   TEXT COMMENT '请求参数说明（JSON）',
    `response_example` TEXT COMMENT '响应示例（JSON）',
    `status`           TINYINT      DEFAULT 0 COMMENT '状态：0下线 1上线',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`        TINYINT      DEFAULT 0 COMMENT '是否删除：0否 1是',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口信息表';

-- 接口订阅表（开发者申请订阅接口，管理员审批后才有调用权限）
CREATE TABLE IF NOT EXISTS `interface_subscribe`
(
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
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
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `interface_id` BIGINT   NOT NULL COMMENT '接口 ID',
    `app_id`       BIGINT   NOT NULL COMMENT '应用 ID',
    `user_id`      BIGINT   NOT NULL COMMENT '用户 ID',
    `success`      TINYINT  DEFAULT 0 COMMENT '是否成功：0失败 1成功',
    `cost_ms`      BIGINT   DEFAULT 0 COMMENT '调用耗时（毫秒）',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_interface_time` (`interface_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口调用日志表';

-- 演示数据
INSERT INTO `user` (`user_account`, `user_password`, `user_name`, `user_role`)
VALUES ('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', '管理员', 'admin');

INSERT INTO `app` (`app_name`, `access_key`, `secret_key`, `user_id`, `status`)
VALUES ('演示应用', 'demo-access-key', 'demo-secret-key', 1, 1);

INSERT INTO `interface_info` (`name`, `description`, `method`, `url`, `request_params`, `response_example`, `status`)
VALUES ('随机名称', '随机返回一个英文名', 'GET', '/api/demo/name', '{"prefix":"名称前缀(可选)"}',
        '{"code":0,"data":"Alice"}', 1),
       ('参数回显', '原样返回表单参数', 'POST', '/api/demo/echo', '{"任意参数":"原样回显"}',
        '{"code":0,"data":{}}', 1);

-- 演示应用默认已订阅两个演示接口（审批通过），保证 CI 测试可直接调用
INSERT INTO `interface_subscribe` (`interface_id`, `app_id`, `user_id`, `status`)
VALUES (1, 1, 1, 1),
       (2, 1, 1, 1);
