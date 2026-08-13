package com.openapi.orderdemo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 订单演示接口（模拟真实上游业务）。
 *
 * <p>支持三个演示开关：</p>
 * <ul>
 *   <li>{@code delay}：休眠指定毫秒，模拟「上游慢」，用于演示超时</li>
 *   <li>{@code fail=true}：返回 500，模拟「上游挂了」，用于演示重试与熔断</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @GetMapping("/query")
    public Map<String, Object> query(
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "0") long delay,
            @RequestParam(defaultValue = "false") boolean fail) throws InterruptedException {
        if (delay > 0) {
            Thread.sleep(delay);
        }
        if (fail) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "模拟上游故障（500）");
        }
        long orderId = id == null ? 1001 : id;
        return Map.of(
                "id", orderId,
                "orderNo", "ORD-" + System.currentTimeMillis(),
                "status", "PAID",
                "amount", 99.9,
                "createTime", "2026-08-14 12:00:00");
    }

    @GetMapping("/list")
    public List<Map<String, Object>> list() {
        return List.of(
                Map.of("id", 1001, "orderNo", "ORD-1001", "status", "PAID"),
                Map.of("id", 1002, "orderNo", "ORD-1002", "status", "SHIPPED"),
                Map.of("id", 1003, "orderNo", "ORD-1003", "status", "CANCELLED"));
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return Map.of("code", 0, "message", "订单创建成功（模拟）", "data", body);
    }
}
