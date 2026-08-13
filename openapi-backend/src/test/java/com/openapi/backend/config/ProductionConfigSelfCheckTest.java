package com.openapi.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionConfigSelfCheckTest {

    @Test
    void collectIssues_detectsDefaultCredentials() {
        List<String> issues = ProductionConfigSelfCheck.collectIssues(
                ProductionConfigSelfCheck.DEFAULT_JWT_SECRET, "123456", "guest", "guest", "", false, false);
        assertEquals(4, issues.size(), "应检测出 JWT/DB/RabbitMQ/Redis 四项默认凭据");
    }

    @Test
    void collectIssues_returnsEmptyWhenAllOverridden() {
        List<String> issues = ProductionConfigSelfCheck.collectIssues(
                "custom-jwt-secret", "Str0ng@Pass", "app", "app-pass", "redis-pass", true, true);
        assertTrue(issues.isEmpty());
    }

    @Test
    void collectIssues_requiresCookieSecureInProduction() {
        List<String> issues = ProductionConfigSelfCheck.collectIssues(
                "custom-jwt-secret", "p", "u", "p", "r", false, true);
        assertTrue(issues.stream().anyMatch(s -> s.contains("Cookie Secure")));
    }

    @Test
    void run_throwsInProductionModeWithDefaults() {
        ProductionConfigSelfCheck check = new ProductionConfigSelfCheck();
        ReflectionTestUtils.setField(check, "jwtSecret", ProductionConfigSelfCheck.DEFAULT_JWT_SECRET);
        ReflectionTestUtils.setField(check, "dbPassword", "123456");
        ReflectionTestUtils.setField(check, "rabbitUser", "guest");
        ReflectionTestUtils.setField(check, "rabbitPassword", "guest");
        ReflectionTestUtils.setField(check, "redisPassword", "");
        ReflectionTestUtils.setField(check, "cookieSecure", false);
        ReflectionTestUtils.setField(check, "productionMode", true);

        assertThrows(IllegalStateException.class, () -> check.run(null));
    }
}
