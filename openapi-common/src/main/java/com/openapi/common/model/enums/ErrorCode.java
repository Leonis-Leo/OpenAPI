package com.openapi.common.model.enums;

import lombok.Getter;

/**
 * 统一错误码。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),

    PARAMS_ERROR(40001, "请求参数错误"),

    SIGN_HEADER_MISSING(40100, "缺少签名请求头"),
    TIMESTAMP_EXPIRED(40101, "请求时间戳已过期"),
    INVALID_ACCESS_KEY(40102, "无效的 AccessKey"),
    NONCE_REUSED(40103, "检测到请求重放"),
    SIGN_ERROR(40104, "签名校验失败"),
    NO_AUTH(40105, "无权限"),

    APP_NOT_FOUND(40401, "应用不存在"),

    NO_SUBSCRIBE(40300, "未订阅该接口或订阅未通过审批"),

    SYSTEM_ERROR(50000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
