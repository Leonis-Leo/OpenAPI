package com.openapi.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiDocConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("OpenAPI 开放平台接口文档")
                .description("基于 OpenAPI 3.0 规范的开放平台接口文档")
                .version("0.0.1-SNAPSHOT"));
    }
}
