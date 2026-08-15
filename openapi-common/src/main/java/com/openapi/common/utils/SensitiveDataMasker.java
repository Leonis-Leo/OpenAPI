package com.openapi.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 敏感字段脱敏工具。
 *
 * <p>用于日志与审计数据落库前，将密码、密钥、Token、Cookie 等字段值替换为掩码。</p>
 */
public final class SensitiveDataMasker {

    public static final String MASK = "***";

    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = Set.of(
            "password", "passwd", "pwd", "secret", "token",
            "authorization", "cookie", "set-cookie", "x-csrf-token");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SensitiveDataMasker() {
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase();
        return SENSITIVE_KEY_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    /**
     * 将 JSON 字符串中的敏感字段递归替换为掩码；解析失败时返回原值。
     */
    public static String maskJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            maskNode(root);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    public static Map<String, String> maskMap(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((key, value) -> result.put(key, isSensitiveKey(key) ? MASK : value));
        return result;
    }

    private static void maskNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode child = objectNode.get(fieldName);
                if (isSensitiveKey(fieldName)) {
                    objectNode.put(fieldName, MASK);
                } else {
                    maskNode(child);
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(SensitiveDataMasker::maskNode);
        }
    }
}
