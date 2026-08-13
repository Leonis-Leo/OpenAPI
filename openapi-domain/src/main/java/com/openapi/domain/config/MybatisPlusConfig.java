package com.openapi.domain.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 雪花 ID 生成器：workerId / datacenterId 从环境变量注入（0~31）。
     * 多实例部署时需为每个实例分配唯一值，避免同机多实例因默认 MAC+PID 推导撞车导致主键冲突。
     */
    @Bean
    public IdentifierGenerator identifierGenerator(
            @Value("${openapi.snowflake.worker-id:0}") long workerId,
            @Value("${openapi.snowflake.datacenter-id:0}") long datacenterId) {
        return new DefaultIdentifierGenerator(workerId, datacenterId);
    }
}
