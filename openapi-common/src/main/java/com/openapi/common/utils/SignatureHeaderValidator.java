package com.openapi.common.utils;

import com.openapi.common.model.enums.ErrorCode;

/**
 * 签名请求头校验：校验签名头完整性与时间戳。
 *
 * <p>网关与后端共用此校验，保证两处对「缺少签名头 / 时间戳过期」的判定逻辑一致，
 * 避免一方放行另一方拒绝的不一致。</p>
 */
public final class SignatureHeaderValidator {

    private SignatureHeaderValidator() {
    }

    /**
     * 校验签名请求头完整性与时间戳有效期。
     *
     * @param accessKey           X-Access-Key
     * @param timestamp           X-Timestamp（毫秒）
     * @param nonce               X-Nonce
     * @param signature           X-Signature
     * @param maxClockSkewMillis  允许的最大时间偏差（毫秒）
     * @return 校验失败返回对应错误码；校验通过返回 {@code null}
     */
    public static ErrorCode validate(String accessKey, String timestamp, String nonce, String signature,
                                     long maxClockSkewMillis) {
        if (isBlank(accessKey) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            return ErrorCode.SIGN_HEADER_MISSING;
        }
        if (SignatureUtils.isTimestampExpired(timestamp, maxClockSkewMillis)) {
            return ErrorCode.TIMESTAMP_EXPIRED;
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
