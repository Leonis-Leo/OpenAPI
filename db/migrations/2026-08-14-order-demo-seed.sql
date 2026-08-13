USE `openapi`;

-- 订单查询演示接口（id=100）与演示应用订阅（幂等）
INSERT INTO `interface_info` (`id`, `name`, `description`, `method`, `url`, `request_params`, `response_example`, `upstream`, `timeout_ms`, `retry_count`, `status`)
VALUES (100, '订单查询（上游演示）', '代理转发到 order-demo-service 的订单查询', 'GET', '/api/order/query',
        '{"id":"订单ID(可选)","delay":"模拟上游慢的毫秒数","fail":"true 模拟 500"}',
        '{"id":1001,"orderNo":"ORD-...","status":"PAID","amount":99.9}',
        'http://localhost:8103', 3000, 1, 1)
ON DUPLICATE KEY UPDATE `upstream` = VALUES(`upstream`), `timeout_ms` = VALUES(`timeout_ms`),
        `retry_count` = VALUES(`retry_count`), `status` = VALUES(`status`);

INSERT INTO `interface_subscribe` (`id`, `interface_id`, `app_id`, `user_id`, `status`)
VALUES (100, 100, 1, 1, 1)
ON DUPLICATE KEY UPDATE `status` = 1;
