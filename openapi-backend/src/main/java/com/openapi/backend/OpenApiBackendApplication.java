package com.openapi.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.openapi.backend.mapper")
public class OpenApiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenApiBackendApplication.class, args);
    }
}
