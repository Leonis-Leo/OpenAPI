package com.openapi.backend.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 密码工具（骨架阶段使用 SHA-256，生产环境请替换为 BCrypt 等加盐哈希）。
 */
public final class PasswordUtils {

    private PasswordUtils() {
    }

    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("不支持的哈希算法", e);
        }
    }
}
