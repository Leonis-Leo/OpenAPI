package com.openapi.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CsrfInterceptor csrfInterceptor;
    private final AuditLogInterceptor auditLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/user/login", "/v1/user/register");
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/user/login", "/v1/user/register");
        registry.addInterceptor(auditLogInterceptor).addPathPatterns("/v1/**");
    }
}
