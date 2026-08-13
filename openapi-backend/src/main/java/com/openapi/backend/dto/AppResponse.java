package com.openapi.backend.dto;

import com.openapi.domain.entity.App;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Application response model. The full SecretKey is returned only after create/reset.
 */
@Data
public class AppResponse {

    private Long id;
    private String appName;
    private String accessKey;
    private String secretKey;
    private String secretKeyHint;
    private Long userId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AppResponse from(App app, boolean exposeSecret) {
        AppResponse response = new AppResponse();
        response.id = app.getId();
        response.appName = app.getAppName();
        response.accessKey = app.getAccessKey();
        response.userId = app.getUserId();
        response.status = app.getStatus();
        response.createTime = app.getCreateTime();
        response.updateTime = app.getUpdateTime();
        response.secretKey = exposeSecret ? app.getSecretKey() : null;
        response.secretKeyHint = mask(app.getSecretKey());
        return response;
    }

    private static String mask(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            return "";
        }
        if (secretKey.length() <= 8) {
            return "********";
        }
        return secretKey.substring(0, 4) + "****" + secretKey.substring(secretKey.length() - 4);
    }
}
