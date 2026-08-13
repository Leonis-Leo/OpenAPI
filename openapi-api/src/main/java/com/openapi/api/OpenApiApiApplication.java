package com.openapi.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据面服务：对外发布的 API 服务端（/api/**），经网关路由，受签名鉴权保护。
 */
@SpringBootApplication(scanBasePackages = {"com.openapi.api", "com.openapi.domain"})
@MapperScan("com.openapi.domain.mapper")
public class OpenApiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenApiApiApplication.class, args);
    }
}
