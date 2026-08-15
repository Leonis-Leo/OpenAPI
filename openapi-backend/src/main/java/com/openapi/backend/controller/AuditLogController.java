package com.openapi.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import com.openapi.domain.entity.AuditLog;
import com.openapi.domain.entity.User;
import com.openapi.domain.mapper.AuditLogMapper;
import com.openapi.domain.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 审计日志查询与导出（管理员）。
 */
@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@Tag(name = "审计日志")
public class AuditLogController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;
    private final UserService userService;

    @GetMapping("/list")
    @Operation(summary = "审计日志列表（管理员）")
    public ApiResponse<Map<String, Object>> list(
            @Parameter(example = "1") @RequestParam(defaultValue = "1") int current,
            @Parameter(example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索：资源路径 / IP") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "动作") @RequestParam(required = false) String action,
            @Parameter(description = "资源路径") @RequestParam(required = false) String resource,
            @Parameter(description = "结果：1 成功 / 0 失败") @RequestParam(required = false) Integer success,
            @Parameter(description = "开始时间 yyyy-MM-dd HH:mm:ss") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss") @RequestParam(required = false) String endTime,
            HttpServletRequest request) {
        requireAdmin(request);
        Page<AuditLog> page = auditLogMapper.selectPage(new Page<>(current, size), buildWrapper(
                keyword, userId, action, resource, success, startTime, endTime));
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(this::enrich).toList());
        result.put("total", page.getTotal());
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "审计日志详情（管理员）")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        AuditLog log = auditLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审计日志不存在");
        }
        return ApiResponse.ok(enrich(log));
    }

    @GetMapping("/export")
    @Operation(summary = "审计日志导出 CSV（管理员）")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "搜索：资源路径 / IP") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "动作") @RequestParam(required = false) String action,
            @Parameter(description = "资源路径") @RequestParam(required = false) String resource,
            @Parameter(description = "结果：1 成功 / 0 失败") @RequestParam(required = false) Integer success,
            @Parameter(description = "开始时间 yyyy-MM-dd HH:mm:ss") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss") @RequestParam(required = false) String endTime,
            HttpServletRequest request) {
        requireAdmin(request);
        List<AuditLog> logs = auditLogMapper.selectList(buildWrapper(
                keyword, userId, action, resource, success, startTime, endTime));
        byte[] csv = buildCsv(logs);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-log.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    private LambdaQueryWrapper<AuditLog> buildWrapper(
            String keyword, Long userId, String action, String resource,
            Integer success, String startTime, String endTime) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AuditLog::getResource, keyword).or().like(AuditLog::getIp, keyword));
        }
        if (userId != null) {
            wrapper.eq(AuditLog::getUserId, userId);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(AuditLog::getAction, action.toUpperCase());
        }
        if (StringUtils.hasText(resource)) {
            wrapper.like(AuditLog::getResource, resource);
        }
        if (success != null) {
            wrapper.eq(AuditLog::getSuccess, success);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(AuditLog::getCreateTime, LocalDateTime.parse(startTime, TIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(AuditLog::getCreateTime, LocalDateTime.parse(endTime, TIME_FORMATTER));
        }
        wrapper.orderByDesc(AuditLog::getId);
        return wrapper;
    }

    private Map<String, Object> enrich(AuditLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUserId());
        map.put("action", log.getAction());
        map.put("resource", log.getResource());
        map.put("ip", log.getIp());
        map.put("statusCode", log.getStatusCode());
        map.put("success", log.getSuccess());
        map.put("detail", log.getDetail());
        map.put("createTime", log.getCreateTime());
        User user = log.getUserId() == null ? null : userMapper.selectById(log.getUserId());
        map.put("userAccount", user == null ? "-" : user.getUserAccount());
        return map;
    }

    private byte[] buildCsv(List<AuditLog> logs) {
        Set<Long> userIds = logs.stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userAccountMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(user ->
                    userAccountMap.put(user.getId(), user.getUserAccount()));
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("ID,用户ID,用户账号,动作,资源,IP,状态码,是否成功,详情,创建时间\n");
        for (AuditLog log : logs) {
            sb.append(log.getId()).append(',')
                    .append(nullToEmpty(log.getUserId())).append(',')
                    .append(csvEscape(log.getUserId() == null ? "-" : userAccountMap.getOrDefault(log.getUserId(), "-"))).append(',')
                    .append(csvEscape(log.getAction())).append(',')
                    .append(csvEscape(log.getResource())).append(',')
                    .append(csvEscape(log.getIp())).append(',')
                    .append(nullToEmpty(log.getStatusCode())).append(',')
                    .append(nullToEmpty(log.getSuccess())).append(',')
                    .append(csvEscape(log.getDetail())).append(',')
                    .append(log.getCreateTime() == null ? "" : log.getCreateTime().format(TIME_FORMATTER))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void requireAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        User user = userService.getById(userId);
        if (user == null || !"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "仅管理员可操作");
        }
    }
}
