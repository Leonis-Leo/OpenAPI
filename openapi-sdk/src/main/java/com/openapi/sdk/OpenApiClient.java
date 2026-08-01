package com.openapi.sdk;

import com.openapi.common.constant.SignConstant;
import com.openapi.common.utils.SignatureUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 开放平台调用方 SDK。
 *
 * <p>自动完成请求签名：拼接参数（含 timestamp、nonce）→ HMAC-SHA256 签名 →
 * 通过请求头携带 AccessKey / timestamp / nonce / signature 调用网关。</p>
 */
public class OpenApiClient {

    private final String baseUrl;
    private final String accessKey;
    private final String secretKey;
    private final HttpClient httpClient;

    public OpenApiClient(String baseUrl, String accessKey, String secretKey) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * GET 调用。
     *
     * @param path  接口路径，如 /api/demo/name
     * @param params 查询参数（可为空）
     * @return 响应体 JSON 字符串
     */
    public String get(String path, Map<String, Object> params) {
        return invoke("GET", path, params);
    }

    /**
     * POST 调用（表单提交）。
     */
    public String post(String path, Map<String, Object> params) {
        return invoke("POST", path, params);
    }

    private String invoke(String method, String path, Map<String, Object> params) {
        Map<String, String> requestParams = normalize(params);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");

        // 签名参数 = 业务参数 + timestamp + nonce
        Map<String, String> signParams = new LinkedHashMap<>(requestParams);
        signParams.put(SignConstant.PARAM_TIMESTAMP, timestamp);
        signParams.put(SignConstant.PARAM_NONCE, nonce);
        String signContent = SignatureUtils.buildSignContent(method, path, signParams);
        String signature = SignatureUtils.sign(signContent, secretKey);

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .header(SignConstant.HEADER_ACCESS_KEY, accessKey)
                    .header(SignConstant.HEADER_TIMESTAMP, timestamp)
                    .header(SignConstant.HEADER_NONCE, nonce)
                    .header(SignConstant.HEADER_SIGNATURE, signature)
                    .timeout(Duration.ofSeconds(10));

            if ("GET".equalsIgnoreCase(method)) {
                String url = baseUrl + path + (requestParams.isEmpty() ? "" : "?" + encodeQuery(requestParams));
                builder.uri(URI.create(url)).GET();
            } else {
                builder.uri(URI.create(baseUrl + path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(encodeQuery(requestParams)));
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用开放平台接口失败", e);
        }
    }

    private static Map<String, String> normalize(Map<String, Object> params) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (params != null) {
            params.forEach((key, value) -> normalized.put(key, value == null ? "" : String.valueOf(value)));
        }
        return normalized;
    }

    private static String encodeQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
