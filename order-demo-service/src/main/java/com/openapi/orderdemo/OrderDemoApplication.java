package com.openapi.orderdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单演示上游服务（:8103）。
 *
 * <p>模拟一个真实的业务后端，供开放平台通过「上游代理」转发调用，
 * 并可配合 delay / fail 参数演示超时、重试与熔断。</p>
 */
@SpringBootApplication
public class OrderDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderDemoApplication.class, args);
    }
}
