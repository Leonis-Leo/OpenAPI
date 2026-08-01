package com.openapi.common.utils;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密钥生成工具，用于生成应用的 AccessKey / SecretKey。
 */
public final class KeyGeneratorUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private KeyGeneratorUtils() {
    }

    /**
     * 生成指定字节数的随机十六进制字符串。
     */
    public static String generateKey(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
