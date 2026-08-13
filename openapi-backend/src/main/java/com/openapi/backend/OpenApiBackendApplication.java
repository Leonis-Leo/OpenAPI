package com.openapi.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.openapi.backend", "com.openapi.domain"})
@MapperScan("com.openapi.domain.mapper")
@EnableScheduling
public class OpenApiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenApiBackendApplication.class, args);
    }
}
