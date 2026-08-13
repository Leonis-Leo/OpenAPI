package com.openapi.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyGeneratorUtilsTest {

    @Test
    void generateKey_returnsHexOfTwiceByteLength() {
        assertEquals(32, KeyGeneratorUtils.generateKey(16).length(), "16 字节 → 32 位十六进制");
        assertEquals(64, KeyGeneratorUtils.generateKey(32).length(), "32 字节 → 64 位十六进制");
        assertTrue(KeyGeneratorUtils.generateKey(32).matches("^[0-9a-f]{64}$"));
    }

    @Test
    void generateKey_producesRandomDistinctValues() {
        String a = KeyGeneratorUtils.generateKey(32);
        String b = KeyGeneratorUtils.generateKey(32);
        assertEquals(64, a.length());
        assertNotEquals(a, b, "两次生成的密钥应不同");
    }
}
