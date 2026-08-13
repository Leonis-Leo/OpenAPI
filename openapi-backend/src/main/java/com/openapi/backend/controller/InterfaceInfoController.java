package com.openapi.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openapi.domain.entity.InterfaceSubscribe;
import com.openapi.domain.entity.InterfaceInfo;
import com.openapi.domain.entity.InterfaceVersion;
import com.openapi.domain.entity.User;
import com.openapi.backend.service.InterfaceInfoService;
import com.openapi.backend.service.InterfaceSubscribeService;
import com.openapi.backend.service.InterfaceVersionService;
import com.openapi.backend.service.InterfaceGroupService;
import com.openapi.backend.service.InterfaceTagService;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/interface")
@RequiredArgsConstructor
@Tag(name = "接口管理")
public class InterfaceInfoController {
    private static final Set<String> SUPPORTED_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final InterfaceInfoService interfaceInfoService;
    private final InterfaceSubscribeService subscribeService;
    private final InterfaceVersionService versionService;
    private final InterfaceGroupService groupService;
    private final InterfaceTagService tagService;
    private final UserService userService;

    @GetMapping("/list")
    @Operation(summary = "查询已上线接口")
    public ApiResponse<List<InterfaceInfo>> listOnline() {
        return ApiResponse.ok(interfaceInfoService.listOnline().stream().map(interfaceInfoService::enrich).toList());
    }

    @GetMapping("/list-all")
    @Operation(summary = "全部接口列表（管理员）")
    public ApiResponse<List<InterfaceInfo>> listAll(HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(interfaceInfoService.listAll().stream().map(interfaceInfoService::enrich).toList());
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询接口")
    public ApiResponse<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Boolean ungrouped,
            @RequestParam(required = false) Long tagId,
            HttpServletRequest request) {
        User currentUser = userService.getById((Long) request.getAttribute("openapi.userId"));
        boolean admin = currentUser != null && "admin".equals(currentUser.getUserRole());
        LambdaQueryWrapper<InterfaceInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InterfaceInfo::getName, keyword).or().like(InterfaceInfo::getUrl, keyword));
        }
        if (admin && status != null) {
            wrapper.eq(InterfaceInfo::getStatus, status);
        } else if (!admin) {
            wrapper.eq(InterfaceInfo::getStatus, 1);
        }
        if (Boolean.TRUE.equals(ungrouped)) {
            wrapper.isNull(InterfaceInfo::getGroupId);
        } else if (groupId != null) {
            wrapper.eq(InterfaceInfo::getGroupId, groupId);
        }
        if (StringUtils.hasText(method)) {
            wrapper.eq(InterfaceInfo::getMethod, method.toUpperCase());
        }
        if (tagId != null) {
            List<Long> ids = interfaceInfoService.interfaceIdsByTag(tagId);
            wrapper.in(InterfaceInfo::getId, ids.isEmpty() ? List.of(-1L) : ids);
        }
        wrapper.orderByDesc(InterfaceInfo::getId);
        Page<InterfaceInfo> page = interfaceInfoService.page(new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 100)), wrapper);
        page.setRecords(page.getRecords().stream().map(interfaceInfoService::enrich).toList());
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询接口详情")
    public ApiResponse<InterfaceInfo> detail(
            @Parameter(description = "接口 ID", example = "1") @PathVariable Long id) {
        InterfaceInfo info = interfaceInfoService.getById(id);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        return ApiResponse.ok(interfaceInfoService.enrich(info));
    }

    @PostMapping("/online")
    @Operation(summary = "接口上线（管理员）")
    public ApiResponse<Void> online(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        interfaceInfoService.online(id, (Long) request.getAttribute("openapi.userId"));
        return ApiResponse.ok();
    }

    @PostMapping("/offline")
    @Operation(summary = "接口下线（管理员）")
    public ApiResponse<Void> offline(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        interfaceInfoService.offline(id);
        return ApiResponse.ok();
    }

    @PostMapping("/subscribe")
    @Operation(summary = "订阅接口")
    public ApiResponse<InterfaceSubscribe> subscribe(@RequestParam Long interfaceId,
                                                     @RequestParam Long appId,
                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        return ApiResponse.ok(subscribeService.subscribe(userId, interfaceId, appId));
    }

    @GetMapping("/my-subscribes")
    @Operation(summary = "我的订阅列表")
    public ApiResponse<List<Map<String, Object>>> mySubscribes(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        return ApiResponse.ok(subscribeService.listByUser(userId));
    }

    @GetMapping("/my-subscribes/page")
    @Operation(summary = "我的订阅分页列表")
    public ApiResponse<Map<String, Object>> mySubscribesPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        return pageResponse(subscribeService.pageByUser(userId, current, size));
    }

    @GetMapping("/subscribes")
    @Operation(summary = "订阅审批列表（管理员）")
    public ApiResponse<List<Map<String, Object>>> subscribes(
            @Parameter(description = "状态：0待审批 1通过 2拒绝", example = "0")
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(subscribeService.listByStatus(status));
    }

    @GetMapping("/subscribes/page")
    @Operation(summary = "订阅审批分页列表（管理员）")
    public ApiResponse<Map<String, Object>> subscribesPage(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest request) {
        requireAdmin(request);
        return pageResponse(subscribeService.pageByStatus(status, current, size));
    }

    @PostMapping("/approve")
    @Operation(summary = "订阅审批（管理员）")
    public ApiResponse<Void> approve(@RequestParam Long id,
                                     @Parameter(example = "true") @RequestParam Boolean approved,
                                     HttpServletRequest request) {
        requireAdmin(request);
        subscribeService.approve(id, approved);
        return ApiResponse.ok();
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "取消订阅")
    public ApiResponse<Void> unsubscribe(@RequestParam Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        subscribeService.unsubscribe(userId, id);
        return ApiResponse.ok();
    }

    @PostMapping("/subscribe-delete")
    @Operation(summary = "删除订阅记录（管理员）")
    public ApiResponse<Void> deleteSubscribe(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        subscribeService.removeById(id);
        return ApiResponse.ok();
    }

    @PostMapping("/create")
    @Operation(summary = "新增接口（管理员）")
    public ApiResponse<InterfaceInfo> create(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String method,
            @RequestParam String url,
            @RequestParam(required = false) String requestParams,
            @RequestParam(required = false) String responseExample,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String tags,
            HttpServletRequest request) {
        requireAdmin(request);
        InterfaceInfo info = new InterfaceInfo();
        info.setName(name);
        info.setDescription(description);
        info.setMethod(normalizeMethod(method));
        info.setUrl(url);
        info.setRequestParams(requestParams);
        info.setResponseExample(responseExample);
        info.setGroupId(groupId);
        info.setStatus(0);
        info.setIsDelete(0);
        interfaceInfoService.save(info);
        if (tags != null) {
            interfaceInfoService.updateWithVersion(info, tags, "创建", (Long) request.getAttribute("openapi.userId"));
        }
        return ApiResponse.ok(info);
    }

    @PostMapping("/update")
    @Operation(summary = "编辑接口（管理员）")
    public ApiResponse<Void> update(
            @RequestParam Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String requestParams,
            @RequestParam(required = false) String responseExample,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String tags,
            HttpServletRequest request) {
        requireAdmin(request);
        InterfaceInfo info = interfaceInfoService.getById(id);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        if (StringUtils.hasText(name)) {
            info.setName(name);
        }
        if (StringUtils.hasText(description)) {
            info.setDescription(description);
        }
        if (StringUtils.hasText(method)) {
            info.setMethod(normalizeMethod(method));
        }
        if (StringUtils.hasText(url)) {
            info.setUrl(url);
        }
        if (requestParams != null) {
            info.setRequestParams(requestParams);
        }
        if (responseExample != null) {
            info.setResponseExample(responseExample);
        }
        if (groupId != null) {
            info.setGroupId(groupId);
        }
        interfaceInfoService.updateWithVersion(info, tags, "在线更新", (Long) request.getAttribute("openapi.userId"));
        return ApiResponse.ok();
    }

    @GetMapping("/groups")
    @Operation(summary = "接口分组树（含总数与未分组数）")
    public ApiResponse<Map<String, Object>> groups() {
        return ApiResponse.ok(groupService.groupTree());
    }

    @PostMapping("/group/create")
    @Operation(summary = "新建分组（管理员）")
    public ApiResponse<Void> createGroup(
            @RequestParam String name,
            @RequestParam(required = false) Long parentId,
            HttpServletRequest request) {
        requireAdmin(request);
        groupService.createGroup(name, parentId);
        return ApiResponse.ok();
    }

    @PostMapping("/group/update")
    @Operation(summary = "重命名分组（管理员）")
    public ApiResponse<Void> updateGroup(@RequestParam Long id, @RequestParam String name, HttpServletRequest request) {
        requireAdmin(request);
        groupService.updateGroup(id, name);
        return ApiResponse.ok();
    }

    @PostMapping("/group/delete")
    @Operation(summary = "删除分组（管理员）")
    public ApiResponse<Void> deleteGroup(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        groupService.deleteGroup(id);
        return ApiResponse.ok();
    }

    @GetMapping("/tags")
    @Operation(summary = "接口标签列表")
    public ApiResponse<List<Map<String, Object>>> tags() {
        return ApiResponse.ok(tagService.listWithCounts());
    }

    @PostMapping("/tag/create")
    @Operation(summary = "新建标签（管理员）")
    public ApiResponse<com.openapi.domain.entity.InterfaceTag> createTag(
            @RequestParam String name, HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(tagService.createTag(name));
    }

    @PostMapping("/tag/delete")
    @Operation(summary = "删除标签（管理员）")
    public ApiResponse<Void> deleteTag(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        tagService.deleteTag(id);
        return ApiResponse.ok();
    }

    @PostMapping("/openapi/import")
    @Operation(summary = "导入 OpenAPI JSON/YAML（管理员）")
    public ApiResponse<Map<String, Object>> openapiImport(
            @RequestParam String spec, HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(interfaceInfoService.openapiImport(spec, (Long) request.getAttribute("openapi.userId")));
    }

    @GetMapping("/openapi/export")
    @Operation(summary = "导出 OpenAPI 文档（json/yaml）")
    public ApiResponse<String> openapiExport(
            @RequestParam(defaultValue = "json") String format,
            HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(interfaceInfoService.openapiExport(format));
    }

    @GetMapping("/versions")
    @Operation(summary = "接口发布版本列表（管理员）")
    public ApiResponse<List<InterfaceVersion>> versions(
            @RequestParam Long interfaceId,
            HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.ok(versionService.listByInterface(interfaceId));
    }

    @PostMapping("/rollback")
    @Operation(summary = "一键回滚到指定版本（管理员）")
    public ApiResponse<Void> rollback(
            @RequestParam Long interfaceId,
            @RequestParam Long versionId,
            HttpServletRequest request) {
        requireAdmin(request);
        interfaceInfoService.rollback(interfaceId, versionId, (Long) request.getAttribute("openapi.userId"));
        return ApiResponse.ok();
    }

    private String normalizeMethod(String method) {
        String normalized = method == null ? "" : method.trim().toUpperCase();
        if (!SUPPORTED_METHODS.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 GET、POST、PUT、PATCH、DELETE 请求方法");
        }
        return normalized;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除接口（管理员）")
    public ApiResponse<Void> delete(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        InterfaceInfo info = interfaceInfoService.getById(id);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        interfaceInfoService.removeById(id);
        return ApiResponse.ok();
    }

    private void requireAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("openapi.userId");
        User user = userService.getById(userId);
        if (user == null || !"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "仅管理员可操作");
        }
    }

    private ApiResponse<Map<String, Object>> pageResponse(Page<Map<String, Object>> page) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        return ApiResponse.ok(result);
    }
}
