package com.openapi.common.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureUtilsTest {

    private static final String SECRET = "SKsecret1234567890";

    @Test
    void sign_returnsLowercaseHexHmacSha256() {
        String sig = SignatureUtils.sign("hello", SECRET);
        assertEquals(64, sig.length(), "HMAC-SHA256 输出应为 64 位十六进制");
        assertTrue(sig.matches("^[0-9a-f]{64}$"), "输出应为小写十六进制");
    }

    @Test
    void verify_acceptsCorrectSignature() {
        String content = "GET\n/api/demo/name\nnonce=n&timestamp=1";
        String sig = SignatureUtils.sign(content, SECRET);
        assertTrue(SignatureUtils.verify(content, SECRET, sig));
    }

    @Test
    void verify_rejectsWrongKeyOrTamperedContent() {
        String content = "GET\n/api/demo/name\nnonce=n&timestamp=1";
        String sig = SignatureUtils.sign(content, SECRET);
        assertFalse(SignatureUtils.verify(content, SECRET + "x", sig), "密钥错误应拒绝");
        assertFalse(SignatureUtils.verify(content + "\nextra", SECRET, sig), "内容被篡改应拒绝");
    }

    @Test
    void buildSignContent_sortsParamsByKeyAndUpperCasesMethod() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("b", "2");
        params.put("a", "1");
        params.put("timestamp", "1000");
        params.put("nonce", "n");
        String content = SignatureUtils.buildSignContent("get", "/api/demo", params);
        assertEquals("GET\n/api/demo\na=1&b=2&nonce=n&timestamp=1000", content);
    }

    @Test
    void buildSignContent_treatsNullValueAsEmptyString() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("a", null);
        assertEquals("GET\n/api\na=", SignatureUtils.buildSignContent("GET", "/api", params));
    }

    @Test
    void isTimestampExpired_withinSkewReturnsFalse() {
        long now = System.currentTimeMillis();
        assertFalse(SignatureUtils.isTimestampExpired(String.valueOf(now), 300000));
    }

    @Test
    void isTimestampExpired_beyondSkewReturnsTrue() {
        long past = System.currentTimeMillis() - 400000;
        assertTrue(SignatureUtils.isTimestampExpired(String.valueOf(past), 300000));
    }

    @Test
    void isTimestampExpired_nonNumericOrNullReturnsTrue() {
        assertTrue(SignatureUtils.isTimestampExpired("abc", 300000));
        assertTrue(SignatureUtils.isTimestampExpired(null, 300000));
    }
}
