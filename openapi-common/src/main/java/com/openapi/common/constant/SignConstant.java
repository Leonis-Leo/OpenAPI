package com.openapi.common.constant;

/**
 * 签名相关常量。
 */
public final class SignConstant {

    /** 请求头：AccessKey */
    public static final String HEADER_ACCESS_KEY = "X-Access-Key";
    /** 请求头：请求时间戳（毫秒） */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    /** 请求头：随机串，防止重放 */
    public static final String HEADER_NONCE = "X-Nonce";
    /** 请求头：签名 */
    public static final String HEADER_SIGNATURE = "X-Signature";

    /** 签名参数 key：时间戳 */
    public static final String PARAM_TIMESTAMP = "timestamp";
    /** 签名参数 key：nonce */
    public static final String PARAM_NONCE = "nonce";

    /** 默认允许的最大时间偏差（毫秒） */
    public static final long DEFAULT_MAX_CLOCK_SKEW_MILLIS = 5 * 60 * 1000L;

    private SignConstant() {
    }
}
