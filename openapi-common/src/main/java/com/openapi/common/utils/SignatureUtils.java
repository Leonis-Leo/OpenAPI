package com.openapi.common.utils;

import com.openapi.common.constant.SignConstant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 签名工具：HMAC-SHA256 计算与校验。
 *
 * <p>签名内容格式：{@code METHOD\n请求路径\n排序后的k=v参数}，
 * 其中参数包含业务参数、timestamp、nonce，按 key 字典序拼接。</p>
 */
public final class SignatureUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private SignatureUtils() {
    }

    /**
     * 使用 SecretKey 对内容做 HMAC-SHA256 签名，返回小写十六进制字符串。
     */
    public static String sign(String content, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("签名计算失败", e);
        }
    }

    /**
     * 常量时间比对签名，防止时序攻击。
     */
    public static boolean verify(String content, String secretKey, String signature) {
        String expected = sign(content, secretKey);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构造待签名内容：METHOD\n路径\n排序后的参数串。
     *
     * <p>params 中的 null 值按空字符串处理；timestamp 与 nonce 需已放入 params。</p>
     */
    public static String buildSignContent(String method, String path, Map<String, String> params) {
        String sortedParams = new TreeMap<>(params).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue()))
                .collect(Collectors.joining("&"));
        return method.toUpperCase(Locale.ROOT) + "\n" + path + "\n" + sortedParams;
    }

    /**
     * 校验时间戳是否在允许偏差内。
     */
    public static boolean isTimestampExpired(String timestamp, long maxClockSkewMillis) {
        try {
            long ts = Long.parseLong(timestamp);
            return Math.abs(System.currentTimeMillis() - ts) > maxClockSkewMillis;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * 时间戳默认最大偏差，见 {@link SignConstant#DEFAULT_MAX_CLOCK_SKEW_MILLIS}。
     */
    public static boolean isTimestampExpired(String timestamp) {
        return isTimestampExpired(timestamp, SignConstant.DEFAULT_MAX_CLOCK_SKEW_MILLIS);
    }
}
