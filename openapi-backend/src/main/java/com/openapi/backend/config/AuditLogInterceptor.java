package com.openapi.backend.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.common.utils.SensitiveDataMasker;
import com.openapi.domain.entity.AuditLog;
import com.openapi.domain.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.HashMap;
import java.util.Map;
@Slf4j @Component @RequiredArgsConstructor
public class AuditLogInterceptor implements HandlerInterceptor {
 private final AuditLogMapper mapper; private final ObjectMapper objectMapper;
 @Override public void afterCompletion(HttpServletRequest req,HttpServletResponse res,Object handler,Exception ex){try{AuditLog a=new AuditLog();a.setUserId((Long)req.getAttribute("openapi.userId"));a.setAction(req.getMethod());a.setResource(req.getRequestURI());a.setIp(req.getRemoteAddr());a.setStatusCode(res.getStatus());a.setSuccess(res.getStatus()<400?1:0);Map<String,String> p=new HashMap<>();req.getParameterMap().forEach((k,v)->p.put(k,String.join(",",v)));a.setDetail(objectMapper.writeValueAsString(SensitiveDataMasker.maskMap(p)));mapper.insert(a);}catch(Exception e){log.warn("write audit log failed",e);}}
}
