package com.openapi.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产配置启动自检：检测默认凭据 / 不安全配置。
 *
 * <p>对应 P0 清单「生产配置启动自检」。覆盖 JWT 密钥、数据库密码、RabbitMQ 账号、
 * Redis 密码与 Cookie Secure 五项：生产模式（{@code openapi.production-mode=true}）下
 * 发现默认凭据即抛出异常拒绝启动，否则仅告警。</p>
 */
@Slf4j
@Component
public class ProductionConfigSelfCheck implements ApplicationRunner {

    static final String DEFAULT_JWT_SECRET = "openapi-platform-jwt-secret-key-0123456789abcdef0123456789abcdef";

    @Value("${openapi.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitUser;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitPassword;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${openapi.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${openapi.production-mode:false}")
    private boolean productionMode;

    @Override
    public void run(ApplicationArguments args) {
        List<String> issues = collectIssues(jwtSecret, dbPassword, rabbitUser, rabbitPassword,
                redisPassword, cookieSecure, productionMode);
        if (issues.isEmpty()) {
            log.info("生产配置自检通过");
            return;
        }
        String detail = String.join("；", issues);
        if (productionMode) {
            throw new IllegalStateException(
                    "生产配置自检失败（" + detail + "），已拒绝启动。请通过环境变量覆盖默认凭据后重启。");
        }
        log.warn("生产配置自检发现不安全配置（非生产模式仅告警）：{}", detail);
    }

    static List<String> collectIssues(String jwtSecret, String dbPassword, String rabbitUser,
                                      String rabbitPassword, String redisPassword,
                                      boolean cookieSecure, boolean productionMode) {
        List<String> issues = new ArrayList<>();
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            issues.add("JWT 密钥为默认值");
        }
        if ("123456".equals(dbPassword)) {
            issues.add("数据库密码为默认值 123456");
        }
        if ("guest".equals(rabbitUser) && "guest".equals(rabbitPassword)) {
            issues.add("RabbitMQ 使用默认账号 guest/guest");
        }
        if (redisPassword == null || redisPassword.isBlank()) {
            issues.add("Redis 未设置密码");
        }
        if (productionMode && !cookieSecure) {
            issues.add("生产模式未开启 Cookie Secure");
        }
        return issues;
    }
}
