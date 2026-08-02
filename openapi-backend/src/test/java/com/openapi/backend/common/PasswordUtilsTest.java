package com.openapi.backend.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {

    private static final String DEMO_ADMIN_HASH =
            "$2b$10$4SPZdBW1mq9oyas7qVL62ukMG9JB8tJ6fBlwdn6h11UfI0mgfG6SK";

    @BeforeAll
    static void silenceExpectedWarnings() {
        // 负向用例故意传入非 BCrypt/空字符串，触发编码器自身的 WARN，属预期行为
        Logger bcryptLogger = (Logger) LoggerFactory.getLogger(
                "org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder");
        bcryptLogger.setLevel(Level.ERROR);
    }

    @Test
    void encode_producesBcryptHashWithRandomSalt() {
        String hash1 = PasswordUtils.encode("admin");
        String hash2 = PasswordUtils.encode("admin");
        assertTrue(hash1.startsWith("$2"), "哈希应以 $2 开头");
        assertEquals(60, hash1.length(), "BCrypt cost 10 哈希应为 60 字符");
        assertNotEquals(hash1, hash2, "相同密码两次编码盐应不同");
    }

    @Test
    void matches_roundTrip() {
        String hash = PasswordUtils.encode("123456");
        assertTrue(PasswordUtils.matches("123456", hash));
        assertFalse(PasswordUtils.matches("wrong", hash));
    }

    @Test
    void matches_verifiesInitSqlDemoHash() {
        assertTrue(PasswordUtils.matches("admin", DEMO_ADMIN_HASH));
    }

    @Test
    void matches_returnsFalseForNonBcryptFormat() {
        assertFalse(PasswordUtils.matches("admin", "not-a-bcrypt-hash"));
        assertFalse(PasswordUtils.matches("admin", null));
    }

    @Test
    void isLegacySha256_detectsOldFormat() {
        String legacy = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";
        assertTrue(PasswordUtils.isLegacySha256(legacy));
        assertFalse(PasswordUtils.isLegacySha256("$2b$10$4SPZdBW1mq9oyas7qVL62ukMG9JB8tJ6fBlwdn6h11UfI0mgfG6SK"));
        assertFalse(PasswordUtils.isLegacySha256(""));
        assertFalse(PasswordUtils.isLegacySha256(null));
    }

    @Test
    void sha256_stillVerifiesLegacyHash() {
        assertEquals("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918",
                PasswordUtils.sha256("admin"));
    }
}
