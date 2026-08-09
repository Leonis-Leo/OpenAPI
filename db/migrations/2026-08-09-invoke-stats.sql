USE `openapi`;

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

-- 从现有明细日志回填历史聚合（幂等：先清空再插入）
DELETE FROM `invoke_stats_counter`;
DELETE FROM `invoke_stats_daily`;
INSERT INTO `invoke_stats_daily` (`stat_date`, `app_id`, `interface_id`, `total`, `success`, `fail`, `total_cost_ms`, `create_time`, `update_time`)
SELECT DATE(create_time), app_id, interface_id, COUNT(*), SUM(success), COUNT(*) - SUM(success), SUM(cost_ms), NOW(), NOW()
FROM `invoke_log`
GROUP BY DATE(create_time), app_id, interface_id;
INSERT INTO `invoke_stats_counter` (`app_id`, `interface_id`, `total`, `success`, `fail`, `total_cost_ms`, `update_time`)
SELECT app_id, interface_id, COUNT(*), SUM(success), COUNT(*) - SUM(success), SUM(cost_ms), NOW()
FROM `invoke_log`
GROUP BY app_id, interface_id;
