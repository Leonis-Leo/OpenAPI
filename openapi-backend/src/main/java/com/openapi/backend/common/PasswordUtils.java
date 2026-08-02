package com.openapi.backend.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 密码工具：BCrypt 加盐哈希（生产标准，cost 10）。
 * SHA-256 仅保留用于存量哈希的迁移期校验，新数据一律走 BCrypt。
 */
public final class PasswordUtils {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final Pattern LEGACY_SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private PasswordUtils() {
    }

    /** 生成 BCrypt 哈希（cost 10，自动加盐）。 */
    public static String encode(String raw) {
        return ENCODER.encode(raw);
    }

    /** BCrypt 比对；非 BCrypt 格式（含 null）一律返回 false。 */
    public static boolean matches(String raw, String encoded) {
        return ENCODER.matches(raw, encoded);
    }

    /** 是否为旧版 64 位小写 SHA-256 哈希。 */
    public static boolean isLegacySha256(String encoded) {
        return encoded != null && LEGACY_SHA256_PATTERN.matcher(encoded).matches();
    }

    /** 旧版 SHA-256 哈希（仅用于存量数据校验）。 */
    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("不支持的哈希算法", e);
        }
    }
}
