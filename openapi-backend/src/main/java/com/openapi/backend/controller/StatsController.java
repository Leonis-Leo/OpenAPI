package com.openapi.backend.controller;

import com.openapi.backend.mapper.InvokeLogMapper;
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
}
