package com.openapi.backend.controller;

import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.mapper.AppMapper;
import com.openapi.backend.mapper.InvokeStatsMapper;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.entity.App;
import com.openapi.backend.entity.User;
import com.openapi.backend.service.UserService;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/stats")
@RequiredArgsConstructor
@Tag(name = "调用统计")
public class StatsController {

    private final InvokeStatsMapper statsMapper;
    private final InterfaceInfoMapper interfaceInfoMapper;
    private final AppMapper appMapper;
    private final UserService userService;

    @GetMapping("/overview")
    @Operation(summary = "调用统计概览")
    public ApiResponse<Map<String, Object>> overview(HttpServletRequest request) {
        requireAdmin(request);
        Map<String, Object> row = statsMapper.overview();
        long total = ((Number) row.getOrDefault("total", 0)).longValue();
        long success = ((Number) row.getOrDefault("success", 0)).longValue();
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", Math.max(0, total - success));
        result.put("successRate",
                total == 0 ? 0 : Math.round(success * 100.0 / total));
        return ApiResponse.ok(result);
    }

    @GetMapping("/daily")
    @Operation(summary = "近 N 天调用趋势")
    public ApiResponse<List<Map<String, Object>>> daily(
            @Parameter(description = "天数", example = "7")
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest request) {
        requireAdmin(request);
        LocalDate since = LocalDate.now().minusDays(days - 1L);
        return ApiResponse.ok(statsMapper.daily(since));
    }

    @GetMapping("/top-interfaces")
    @Operation(summary = "接口调用排行")
    public ApiResponse<List<Map<String, Object>>> topInterfaces(
            @Parameter(description = "条数", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(statsMapper.topInterfaces(limit).stream().map(row -> {
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
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(statsMapper.topApps(limit).stream().map(row -> {
            Map<String, Object> map = new HashMap<>(row);
            Object id = row.get("appId");
            if (id != null) {
                App app = appMapper.selectById(Long.valueOf(id.toString()));
                map.put("appName", app == null ? "-" : app.getAppName());
            }
            return map;
        }).toList());
    }

    @GetMapping("/daily-page")
    @Operation(summary = "调用明细分页（按天/应用/接口维度）")
    public ApiResponse<Map<String, Object>> dailyPage(
            @Parameter(description = "维度：day/app/interface") @RequestParam(defaultValue = "day") String dimension,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) Long interfaceId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest request) {
        requireAdmin(request);
        String normalized = dimension == null ? "day" : dimension.toLowerCase();
        if (!Set.of("day", "app", "interface").contains(normalized)) {
            normalized = "day";
        }
        com.baomidou.mybatisplus.core.metadata.IPage<Map<String, Object>> page =
                statsMapper.pageDaily(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(Math.max(1, current), Math.min(Math.max(1, size), 100)),
                        normalized,
                        parseDate(startDate),
                        parseDate(endDate),
                        appId,
                        interfaceId);
        List<Map<String, Object>> records = page.getRecords().stream().map(row -> {
            long total = ((Number) row.getOrDefault("total", 0)).longValue();
            long success = ((Number) row.getOrDefault("success", 0)).longValue();
            long totalCostMs = ((Number) row.getOrDefault("totalCostMs", 0)).longValue();
            row.put("successRate", total == 0 ? 0 : Math.round(success * 100.0 / total));
            row.put("avgCostMs", total == 0 ? 0 : Math.round(totalCostMs * 1.0 / total));
            Object appIdObj = row.get("appId");
            if (appIdObj != null) {
                App app = appMapper.selectById(Long.valueOf(appIdObj.toString()));
                row.put("appName", app == null ? "-" : app.getAppName());
            }
            Object interfaceIdObj = row.get("interfaceId");
            if (interfaceIdObj != null) {
                InterfaceInfo info = interfaceInfoMapper.selectById(Long.valueOf(interfaceIdObj.toString()));
                row.put("interfaceName", info == null ? "-" : info.getName());
            }
            return row;
        }).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        return ApiResponse.ok(result);
    }

    private LocalDate parseDate(String value) {
        if (!org.springframework.util.StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void requireAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        User user = userService.getById(userId);
        if (user == null || !"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "仅管理员可查看统计");
        }
    }
}
