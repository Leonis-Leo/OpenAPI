package com.openapi.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.common.utils.SignatureUtils;
import com.openapi.domain.entity.App;
import com.openapi.domain.mapper.AppMapper;
import com.openapi.domain.mapper.InterfaceInfoMapper;
import com.openapi.domain.mapper.InterfaceSubscribeMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureInterceptorTest {

    @Mock private AppMapper appMapper;
    @Mock private InterfaceInfoMapper interfaceInfoMapper;
    @Mock private InterfaceSubscribeMapper subscribeMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SignatureInterceptor interceptor;

    private static final long SKEW = 300000L;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new SignatureInterceptor(appMapper, interfaceInfoMapper, subscribeMapper,
                stringRedisTemplate, objectMapper, rabbitTemplate);
        ReflectionTestUtils.setField(interceptor, "maxClockSkewMillis", SKEW);
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @Test
    void missingHeaderRejected() throws Exception {
        when(request.getHeader(anyString())).thenReturn(null);
        assertFalse(interceptor.preHandle(request, response, null));
        verify(response).setStatus(401);
    }

    @Test
    void expiredTimestampRejected() throws Exception {
        when(request.getHeader("X-Access-Key")).thenReturn("AKdemo");
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(System.currentTimeMillis() - 400000));
        when(request.getHeader("X-Nonce")).thenReturn("n");
        when(request.getHeader("X-Signature")).thenReturn("s");
        assertFalse(interceptor.preHandle(request, response, null));
        verify(response).setStatus(401);
    }

    @Test
    void invalidAccessKeyRejected() throws Exception {
        when(request.getHeader("X-Access-Key")).thenReturn("AKunknown");
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(System.currentTimeMillis()));
        when(request.getHeader("X-Nonce")).thenReturn("n");
        when(request.getHeader("X-Signature")).thenReturn("s");
        when(appMapper.selectOne(any())).thenReturn(null);
        assertFalse(interceptor.preHandle(request, response, null));
        verify(response).setStatus(401);
    }

    @Test
    void validSignaturePasses() throws Exception {
        App app = new App();
        app.setId(1L);
        app.setUserId(42L);
        app.setAccessKey("AKdemo");
        app.setSecretKey("SKsecret123456");
        app.setStatus(1);

        String accessKey = "AKdemo";
        String nonce = "nonce-123";
        String timestamp = String.valueOf(System.currentTimeMillis());
        when(request.getHeader("X-Access-Key")).thenReturn(accessKey);
        when(request.getHeader("X-Timestamp")).thenReturn(timestamp);
        when(request.getHeader("X-Nonce")).thenReturn(nonce);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/demo/name");
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());

        Map<String, String> params = new HashMap<>();
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        String signature = SignatureUtils.sign(
                SignatureUtils.buildSignContent("GET", "/api/demo/name", params), app.getSecretKey());
        when(request.getHeader("X-Signature")).thenReturn(signature);

        when(appMapper.selectOne(any())).thenReturn(app);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(interfaceInfoMapper.selectOne(any())).thenReturn(null);

        assertTrue(interceptor.preHandle(request, response, null));
        verify(request).setAttribute(eq("openapi.app"), eq(app));
    }
}
