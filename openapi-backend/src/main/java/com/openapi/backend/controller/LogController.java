package com.openapi.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openapi.backend.entity.App;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.entity.InvokeLog;
import com.openapi.backend.entity.User;
import com.openapi.backend.mapper.AppMapper;
import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.mapper.InvokeLogMapper;
import com.openapi.backend.mapper.UserMapper;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/log")
@RequiredArgsConstructor
@Tag(name = "API 日志")
public class LogController {

    private final InvokeLogMapper invokeLogMapper;
    private final InterfaceInfoMapper interfaceInfoMapper;
    private final AppMapper appMapper;
    private final UserMapper userMapper;
    private final UserService userService;

    @GetMapping("/list")
    @Operation(summary = "日志列表（管理员）")
    public ApiResponse<Map<String, Object>> list(
            @Parameter(example = "1") @RequestParam(defaultValue = "1") int current,
            @Parameter(example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索：路径 / IP") @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<InvokeLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InvokeLog::getPath, keyword).or().like(InvokeLog::getIp, keyword));
        }
        wrapper.orderByDesc(InvokeLog::getId);
        Page<InvokeLog> page = invokeLogMapper.selectPage(new Page<>(current, size), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(this::enrich).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "日志详情（管理员）")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        InvokeLog log = invokeLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志不存在");
        }
        return ApiResponse.ok(enrich(log));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除日志（管理员）")
    public ApiResponse<Void> delete(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        invokeLogMapper.deleteById(id);
        return ApiResponse.ok();
    }

    @PostMapping("/delete-batch")
    @Operation(summary = "批量删除日志（管理员）")
    public ApiResponse<Void> deleteBatch(@RequestParam String ids, HttpServletRequest request) {
        requireAdmin(request);
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toList();
        invokeLogMapper.deleteBatchIds(idList);
        return ApiResponse.ok();
    }

    private Map<String, Object> enrich(InvokeLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("interfaceId", log.getInterfaceId());
        map.put("appId", log.getAppId());
        map.put("userId", log.getUserId());
        map.put("ip", log.getIp());
        map.put("method", log.getMethod());
        map.put("path", log.getPath());
        map.put("requestParams", log.getRequestParams());
        map.put("responseBody", log.getResponseBody());
        map.put("statusCode", log.getStatusCode());
        map.put("success", log.getSuccess());
        map.put("costMs", log.getCostMs());
        map.put("createTime", log.getCreateTime());
        InterfaceInfo info = interfaceInfoMapper.selectById(log.getInterfaceId());
        map.put("interfaceName", info == null ? "-" : info.getName());
        App app = appMapper.selectById(log.getAppId());
        map.put("appName", app == null ? "-" : app.getAppName());
        User user = userMapper.selectById(log.getUserId());
        map.put("userAccount", user == null ? "-" : user.getUserAccount());
        return map;
    }

    private void requireAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        User user = userService.getById(userId);
        if (user == null || !"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "仅管理员可操作");
        }
    }
}
