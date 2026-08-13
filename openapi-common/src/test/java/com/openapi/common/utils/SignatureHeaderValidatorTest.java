package com.openapi.common.utils;

import com.openapi.common.model.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SignatureHeaderValidatorTest {

    private static final long SKEW = 300000L;

    @Test
    void validate_returnsMissingWhenAnyHeaderBlank() {
        assertEquals(ErrorCode.SIGN_HEADER_MISSING,
                SignatureHeaderValidator.validate(null, "1", "n", "s", SKEW));
        assertEquals(ErrorCode.SIGN_HEADER_MISSING,
                SignatureHeaderValidator.validate("ak", "", "n", "s", SKEW));
        assertEquals(ErrorCode.SIGN_HEADER_MISSING,
                SignatureHeaderValidator.validate("ak", "1", " ", "s", SKEW));
    }

    @Test
    void validate_returnsExpiredWhenTimestampBeyondSkew() {
        String past = String.valueOf(System.currentTimeMillis() - 400000);
        assertEquals(ErrorCode.TIMESTAMP_EXPIRED,
                SignatureHeaderValidator.validate("ak", past, "n", "s", SKEW));
    }

    @Test
    void validate_returnsNullWhenAllValid() {
        String now = String.valueOf(System.currentTimeMillis());
        assertNull(SignatureHeaderValidator.validate("ak", now, "n", "s", SKEW));
    }
}
