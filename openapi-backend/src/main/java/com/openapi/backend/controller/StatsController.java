package com.openapi.backend.controller;

import com.openapi.backend.mapper.InvokeLogMapper;
import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.mapper.AppMapper;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.entity.App;
import com.openapi.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/stats")
@RequiredArgsConstructor
@Tag(name = "调用统计")
public class StatsController {

    private final InvokeLogMapper invokeLogMapper;
    private final InterfaceInfoMapper interfaceInfoMapper;
    private final AppMapper appMapper;

    @GetMapping("/overview")
    @Operation(summary = "调用统计概览")
    public ApiResponse<Map<String, Object>> overview() {
        Long total = invokeLogMapper.countAll();
        Long success = invokeLogMapper.countSuccess();
        Map<String, Object> result = new HashMap<>();
        result.put("total", total == null ? 0 : total);
        result.put("success", success == null ? 0 : success);
        result.put("fail", total == null ? 0 : total - (success == null ? 0 : success));
        result.put("successRate",
                total == null || total == 0 ? 0 : Math.round((success == null ? 0 : success) * 100.0 / total));
        return ApiResponse.ok(result);
    }

    @GetMapping("/daily")
    @Operation(summary = "近 N 天调用趋势")
    public ApiResponse<List<Map<String, Object>>> daily(
            @Parameter(description = "天数", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        return ApiResponse.ok(invokeLogMapper.dailyStats(since));
    }

    @GetMapping("/top-interfaces")
    @Operation(summary = "接口调用排行")
    public ApiResponse<List<Map<String, Object>>> topInterfaces(
            @Parameter(description = "条数", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(invokeLogMapper.statsByInterface(limit).stream().map(row -> {
            Map<String, Object> map = new HashMap<>(row);
            Object id = row.get("interfaceId");
            if (id != null) {
                InterfaceInfo info = interfaceInfoMapper.selectById(Long.valueOf(id.toString()));
                map.put("interfaceName", info == null ? "-" : info.getName());
            }
            return map;
        }).toList());
    }

    @GetMapping("/top-apps")
    @Operation(summary = "应用调用排行")
    public ApiResponse<List<Map<String, Object>>> topApps(
            @Parameter(description = "条数", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(invokeLogMapper.statsByApp(limit).stream().map(row -> {
            Map<String, Object> map = new HashMap<>(row);
            Object id = row.get("appId");
            if (id != null) {
                App app = appMapper.selectById(Long.valueOf(id.toString()));
                map.put("appName", app == null ? "-" : app.getAppName());
            }
            return map;
        }).toList());
    }
}
