package com.openapi.api.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.common.constant.SignConstant;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import com.openapi.common.trace.TraceContext;
import com.openapi.common.utils.SensitiveDataMasker;
import com.openapi.common.utils.SignatureHeaderValidator;
import com.openapi.common.utils.SignatureUtils;
import com.openapi.domain.entity.App;
import com.openapi.domain.entity.InterfaceInfo;
import com.openapi.domain.mapper.AppMapper;
import com.openapi.domain.mapper.InterfaceInfoMapper;
import com.openapi.domain.mapper.InterfaceSubscribeMapper;
import com.openapi.domain.mq.InvokeLogMessage;
import com.openapi.domain.mq.RabbitConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 数据面签名鉴权拦截器：校验签名头完整性与时间戳 → 密钥查询 → nonce 防重放 → 签名比对 → 订阅校验，
 * 并在请求完成后异步发布调用日志到 MQ。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureInterceptor implements HandlerInterceptor {

    private final AppMapper appMapper;
    private final InterfaceInfoMapper interfaceInfoMapper;
    private final InterfaceSubscribeMapper subscribeMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    @Value("${openapi.sign.max-clock-skew-millis:300000}") private long maxClockSkewMillis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String accessKey = request.getHeader(SignConstant.HEADER_ACCESS_KEY);
        String timestamp = request.getHeader(SignConstant.HEADER_TIMESTAMP);
        String nonce = request.getHeader(SignConstant.HEADER_NONCE);
        String signature = request.getHeader(SignConstant.HEADER_SIGNATURE);
        ErrorCode headerError = SignatureHeaderValidator.validate(accessKey, timestamp, nonce, signature, maxClockSkewMillis);
        if (headerError != null) return reject(response, headerError);
        App app = appMapper.selectOne(new LambdaQueryWrapper<App>().eq(App::getAccessKey, accessKey));
        if (app == null || !Integer.valueOf(1).equals(app.getStatus())) return reject(response, ErrorCode.INVALID_ACCESS_KEY);
        try {
            Boolean firstSeen = stringRedisTemplate.opsForValue().setIfAbsent("openapi:nonce:" + accessKey + ":" + nonce, "1", Duration.ofMinutes(5));
            if (Boolean.FALSE.equals(firstSeen)) return reject(response, ErrorCode.NONCE_REUSED);
            if (firstSeen == null) return reject(response, ErrorCode.DEPENDENCY_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Redis unavailable, reject signed request", e);
            return reject(response, ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));
        params.put(SignConstant.PARAM_TIMESTAMP, timestamp);
        params.put(SignConstant.PARAM_NONCE, nonce);
        if (!SignatureUtils.verify(SignatureUtils.buildSignContent(request.getMethod(), request.getRequestURI(), params), app.getSecretKey(), signature)) return reject(response, ErrorCode.SIGN_ERROR);
        InterfaceInfo info = interfaceInfoMapper.selectOne(new LambdaQueryWrapper<InterfaceInfo>().eq(InterfaceInfo::getUrl, request.getRequestURI()).eq(InterfaceInfo::getMethod, request.getMethod()));
        if (info != null && subscribeMapper.countApproved(app.getId(), info.getId()) == 0) return reject(response, ErrorCode.NO_SUBSCRIBE);
        request.setAttribute("openapi.app", app);
        request.setAttribute("openapi.startTime", System.currentTimeMillis());
        request.setAttribute("openapi.interfaceId", info == null ? null : info.getId());
        request.setAttribute("openapi.interfaceInfo", info);
        log.info("signed api request traceId={} appId={} interfaceId={} method={} path={}",
                TraceContext.current(), app.getId(), info == null ? null : info.getId(),
                request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            Long start = (Long) request.getAttribute("openapi.startTime"); App app = (App) request.getAttribute("openapi.app");
            if (start == null || app == null) return;
            InvokeLogMessage m = new InvokeLogMessage(); m.setInterfaceId((Long) request.getAttribute("openapi.interfaceId")); m.setAppId(app.getId()); m.setUserId(app.getUserId()); m.setIp(request.getRemoteAddr()); m.setMethod(request.getMethod()); m.setPath(request.getRequestURI()); m.setRequestParams(buildParams(request)); m.setRequestHeaders(buildHeaders(request)); m.setResponseBody(readBody(response)); m.setStatusCode(response.getStatus()); m.setSuccess(response.getStatus() < 400); m.setCostMs(System.currentTimeMillis() - start); m.setCreateTime(LocalDateTime.now());
            String traceId = TraceContext.current();
            String messageId = UUID.randomUUID().toString().replace("-", "");
            rabbitTemplate.convertAndSend(RabbitConstant.EXCHANGE_INVOKE, RabbitConstant.ROUTING_INVOKE_LOG, m, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                if (traceId != null && !traceId.isBlank()) {
                    msg.getMessageProperties().setHeader(TraceContext.HEADER_TRACE_ID, traceId);
                }
                return msg;
            });
        } catch (Exception e) { log.warn("publish invoke log failed", e); }
    }

    private String buildParams(HttpServletRequest request) { try { Map<String,String> p = new HashMap<>(); request.getParameterMap().forEach((k,v)->p.put(k,String.join(",",v))); return truncate(objectMapper.writeValueAsString(SensitiveDataMasker.maskMap(p))); } catch(Exception e) { return ""; } }
    private String buildHeaders(HttpServletRequest request) {
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            java.util.Enumeration<String> names = request.getHeaderNames();
            while (names != null && names.hasMoreElements()) {
                String name = names.nextElement();
                headers.put(name, request.getHeader(name));
            }
            return truncate(objectMapper.writeValueAsString(SensitiveDataMasker.maskMap(headers)));
        } catch (Exception e) {
            return "";
        }
    }
    private String readBody(HttpServletResponse response) { try { if (response instanceof ContentCachingResponseWrapper w) return truncate(SensitiveDataMasker.maskJson(new String(w.getContentAsByteArray(), StandardCharsets.UTF_8))); } catch(Exception ignored) {} return ""; }
    private static String truncate(String s) { return s != null && s.length() > 2000 ? s.substring(0,2000) : s; }
    private boolean reject(HttpServletResponse response, ErrorCode code) throws IOException { int status = code == ErrorCode.NO_SUBSCRIBE || code == ErrorCode.CSRF_INVALID ? 403 : code == ErrorCode.DEPENDENCY_UNAVAILABLE ? 503 : 401; response.setStatus(status); response.setContentType("application/json;charset=UTF-8"); response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code))); return false; }
}
