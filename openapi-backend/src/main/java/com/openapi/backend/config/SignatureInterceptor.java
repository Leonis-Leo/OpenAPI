package com.openapi.backend.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.backend.entity.App;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.mapper.AppMapper;
import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.service.InterfaceSubscribeService;
import com.openapi.common.constant.SignConstant;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import com.openapi.common.utils.SignatureUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 签名校验拦截器：校验签名头、时间戳与 nonce，并重建签名内容完成比对。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureInterceptor implements HandlerInterceptor {

    private final AppMapper appMapper;
    private final InterfaceInfoMapper interfaceInfoMapper;
    private final InterfaceSubscribeService subscribeService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openapi.sign.max-clock-skew-millis:300000}")
    private long maxClockSkewMillis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String accessKey = request.getHeader(SignConstant.HEADER_ACCESS_KEY);
        String timestamp = request.getHeader(SignConstant.HEADER_TIMESTAMP);
        String nonce = request.getHeader(SignConstant.HEADER_NONCE);
        String signature = request.getHeader(SignConstant.HEADER_SIGNATURE);
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            return reject(response, ErrorCode.SIGN_HEADER_MISSING);
        }

        if (SignatureUtils.isTimestampExpired(timestamp, maxClockSkewMillis)) {
            return reject(response, ErrorCode.TIMESTAMP_EXPIRED);
        }

        App app = appMapper.selectOne(new LambdaQueryWrapper<App>().eq(App::getAccessKey, accessKey));
        if (app == null) {
            return reject(response, ErrorCode.INVALID_ACCESS_KEY);
        }

        // nonce 防重放：同一窗口期内重复使用即拒绝
        Boolean firstSeen = null;
        try {
            firstSeen = stringRedisTemplate.opsForValue()
                    .setIfAbsent("openapi:nonce:" + accessKey + ":" + nonce, "1", Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过 nonce 防重放校验", e);
        }
        if (Boolean.FALSE.equals(firstSeen)) {
            return reject(response, ErrorCode.NONCE_REUSED);
        }

        // 重建签名内容：请求参数 + 请求头中的 timestamp/nonce
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));
        params.put(SignConstant.PARAM_TIMESTAMP, timestamp);
        params.put(SignConstant.PARAM_NONCE, nonce);
        String signContent = SignatureUtils.buildSignContent(request.getMethod(), request.getRequestURI(), params);
        if (!SignatureUtils.verify(signContent, app.getSecretKey(), signature)) {
            return reject(response, ErrorCode.SIGN_ERROR);
        }

        // 订阅权限校验：调用已发布的接口需所属应用已订阅且审批通过
        InterfaceInfo interfaceInfo = interfaceInfoMapper.selectOne(
                new LambdaQueryWrapper<InterfaceInfo>()
                        .eq(InterfaceInfo::getUrl, request.getRequestURI())
                        .eq(InterfaceInfo::getMethod, request.getMethod()));
        if (interfaceInfo != null
                && !subscribeService.hasApprovedSubscription(app.getId(), interfaceInfo.getId())) {
            return reject(response, ErrorCode.NO_SUBSCRIBE);
        }

        request.setAttribute("openapi.app", app);
        return true;
    }

    private boolean reject(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode == ErrorCode.NO_SUBSCRIBE
                ? HttpServletResponse.SC_FORBIDDEN
                : HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorCode)));
        return false;
    }
}
