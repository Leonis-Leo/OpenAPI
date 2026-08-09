package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.backend.entity.InterfaceGroup;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.entity.InterfaceTag;
import com.openapi.backend.entity.InterfaceTagRelation;
import com.openapi.backend.entity.InterfaceVersion;
import com.openapi.backend.mapper.InterfaceGroupMapper;
import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.mapper.InterfaceTagMapper;
import com.openapi.backend.mapper.InterfaceTagRelationMapper;
import com.openapi.backend.service.InterfaceInfoService;
import com.openapi.backend.service.InterfaceVersionService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
        implements InterfaceInfoService {

    private static final Set<String> SUPPORTED_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final InterfaceVersionService versionService;
    private final InterfaceGroupMapper groupMapper;
    private final InterfaceTagMapper tagMapper;
    private final InterfaceTagRelationMapper tagRelationMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<InterfaceInfo> listOnline() {
        return enrichList(lambdaQuery().eq(InterfaceInfo::getStatus, 1).list());
    }

    @Override
    public List<InterfaceInfo> listAll() {
        return enrichList(list());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void online(Long id, Long operatorId) {
        InterfaceInfo info = getById(id);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        info.setStatus(1);
        updateById(info);
        versionService.snapshot(toVersion(info), "上线发布", operatorId);
    }

    @Override
    public void offline(Long id) {
        updateStatus(id, 0);
    }

    private void updateStatus(Long id, int status) {
        InterfaceInfo info = getById(id);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        info.setStatus(status);
        updateById(info);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithVersion(InterfaceInfo info, String tagIds, String changeNote, Long operatorId) {
        updateById(info);
        if (tagIds != null) {
            replaceTags(info.getId(), tagIds);
        }
        InterfaceInfo latest = getById(info.getId());
        if (latest != null && Integer.valueOf(1).equals(latest.getStatus())) {
            versionService.snapshot(toVersion(latest), changeNote, operatorId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long interfaceId, Long versionId, Long operatorId) {
        InterfaceInfo info = getById(interfaceId);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        InterfaceVersion version = versionService.getVersion(interfaceId, versionId);
        info.setName(version.getName());
        info.setDescription(version.getDescription());
        info.setMethod(version.getMethod());
        info.setUrl(version.getUrl());
        info.setRequestParams(version.getRequestParams());
        info.setResponseExample(version.getResponseExample());
        updateById(info);
        versionService.snapshot(toVersion(info), "回滚到 v" + version.getVersionNo(), operatorId);
    }

    @Override
    public List<Long> interfaceIdsByTag(Long tagId) {
        return tagRelationMapper.selectList(
                        new LambdaQueryWrapper<InterfaceTagRelation>().eq(InterfaceTagRelation::getTagId, tagId))
                .stream()
                .map(InterfaceTagRelation::getInterfaceId)
                .toList();
    }

    @Override
    public InterfaceInfo enrich(InterfaceInfo info) {
        if (info == null) {
            return null;
        }
        if (info.getGroupId() != null) {
            InterfaceGroup group = groupMapper.selectById(info.getGroupId());
            info.setGroupName(group == null ? null : group.getName());
        }
        List<Map<String, Object>> tags = new ArrayList<>();
        List<InterfaceTagRelation> relations = tagRelationMapper.selectList(
                new LambdaQueryWrapper<InterfaceTagRelation>()
                        .eq(InterfaceTagRelation::getInterfaceId, info.getId()));
        for (InterfaceTagRelation relation : relations) {
            InterfaceTag tag = tagMapper.selectById(relation.getTagId());
            if (tag != null) {
                Map<String, Object> tagMap = new HashMap<>();
                tagMap.put("id", tag.getId());
                tagMap.put("name", tag.getName());
                tagMap.put("color", tag.getColor());
                tags.add(tagMap);
            }
        }
        info.setTags(tags);
        return info;
    }

    private List<InterfaceInfo> enrichList(List<InterfaceInfo> list) {
        return list.stream().map(this::enrich).toList();
    }

    private void replaceTags(Long interfaceId, String tagIds) {
        tagRelationMapper.delete(new LambdaQueryWrapper<InterfaceTagRelation>()
                .eq(InterfaceTagRelation::getInterfaceId, interfaceId));
        for (String part : tagIds.split(",")) {
            String trimmed = part.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            try {
                Long tagId = Long.valueOf(trimmed);
                if (tagMapper.selectById(tagId) != null) {
                    InterfaceTagRelation relation = new InterfaceTagRelation();
                    relation.setInterfaceId(interfaceId);
                    relation.setTagId(tagId);
                    tagRelationMapper.insert(relation);
                }
            } catch (NumberFormatException ignored) {
                // 忽略无效标签 ID
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> openapiImport(String spec, Long operatorId) {
        if (!StringUtils.hasText(spec)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "OpenAPI 内容不能为空");
        }
        JsonNode root = parseSpec(spec);
        JsonNode paths = root.path("paths");
        if (!paths.isObject()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "OpenAPI 中缺少 paths 定义");
        }
        int created = 0;
        int skipped = 0;
        Map<String, InterfaceGroup> groupCache = new HashMap<>();
        Map<String, InterfaceTag> tagCache = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> pathIter = paths.fields();
        while (pathIter.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathIter.next();
            String pathKey = pathEntry.getKey();
            for (String method : SUPPORTED_METHODS) {
                JsonNode operation = pathEntry.getValue().path(method.toLowerCase());
                if (!operation.isObject()) {
                    continue;
                }
                String url = normalizeUrl(pathKey);
                Long exists = lambdaQuery()
                        .eq(InterfaceInfo::getUrl, url)
                        .eq(InterfaceInfo::getMethod, method)
                        .count();
                if (exists != null && exists > 0) {
                    skipped++;
                    continue;
                }
                InterfaceInfo info = new InterfaceInfo();
                info.setName(operation.path("summary").asText(pathKey));
                info.setDescription(operation.path("description").asText(""));
                info.setMethod(method);
                info.setUrl(url);
                info.setRequestParams(buildRequestParams(operation));
                info.setResponseExample(buildResponseExample(operation));
                info.setStatus(0);
                info.setIsDelete(0);

                String groupName = operation.path("x-group").asText("");
                if (StringUtils.hasText(groupName)) {
                    InterfaceGroup group = groupCache.computeIfAbsent(groupName.trim(),
                            name -> findOrCreateGroup(name));
                    info.setGroupId(group.getId());
                }
                save(info);
                created++;

                JsonNode tagsNode = operation.path("tags");
                if (tagsNode.isArray()) {
                    for (JsonNode tagNode : tagsNode) {
                        String tagName = tagNode.asText("");
                        if (!StringUtils.hasText(tagName)) {
                            continue;
                        }
                        InterfaceTag tag = tagCache.computeIfAbsent(tagName.trim(), name -> findOrCreateTag(name));
                        InterfaceTagRelation relation = new InterfaceTagRelation();
                        relation.setInterfaceId(info.getId());
                        relation.setTagId(tag.getId());
                        tagRelationMapper.insert(relation);
                    }
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        return result;
    }

    @Override
    public String openapiExport(String format) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("openapi", "3.0.1");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "OpenAPI 开放平台接口文档");
        info.put("version", "1.0.0");
        info.put("description", "由 OpenAPI 开放平台导出的接口定义");
        doc.put("info", info);
        doc.put("servers", List.of(Map.of("url", "http://localhost:8080")));

        Map<String, Object> paths = new LinkedHashMap<>();
        List<InterfaceInfo> interfaces = enrichList(list());
        for (InterfaceInfo item : interfaces) {
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(item.getUrl(),
                    key -> new LinkedHashMap<>());
            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("summary", item.getName());
            operation.put("description", item.getDescription() == null ? "" : item.getDescription());
            operation.put("operationId", item.getMethod().toLowerCase() + "-" + item.getName());
            List<String> opTags = new ArrayList<>();
            if (StringUtils.hasText(item.getGroupName())) {
                opTags.add(item.getGroupName());
            }
            if (item.getTags() != null) {
                item.getTags().forEach(tag -> opTags.add(String.valueOf(tag.get("name"))));
            }
            if (!opTags.isEmpty()) {
                operation.put("tags", opTags.stream().distinct().toList());
            }
            operation.put("parameters", parseParameters(item.getRequestParams()));
            Map<String, Object> responses = new LinkedHashMap<>();
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("application/json", Map.of("example", parseExample(item.getResponseExample())));
            responses.put("200", Map.of("description", "成功", "content", content));
            operation.put("responses", responses);
            pathItem.put(item.getMethod().toLowerCase(), operation);
        }
        doc.put("paths", paths);

        try {
            if ("yaml".equalsIgnoreCase(format)) {
                return new Yaml().dump(objectMapper.convertValue(doc, Map.class));
            }
            return objectMapper.writeValueAsString(doc);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OpenAPI 导出失败");
        }
    }

    private JsonNode parseSpec(String spec) {
        try {
            if (spec.trim().startsWith("{")) {
                return objectMapper.readTree(spec);
            }
            Object yaml = new Yaml().load(spec);
            return objectMapper.valueToTree(yaml);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "OpenAPI 内容解析失败：" + e.getMessage());
        }
    }

    private String normalizeUrl(String pathKey) {
        String url = pathKey.trim();
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        return url;
    }

    private String buildRequestParams(JsonNode operation) {
        JsonNode parameters = operation.path("parameters");
        if (!parameters.isArray() || parameters.isEmpty()) {
            return null;
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (JsonNode param : parameters) {
            String name = param.path("name").asText("");
            if (!StringUtils.hasText(name)) {
                continue;
            }
            String in = param.path("in").asText("query");
            String type = param.path("schema").path("type").asText("string");
            String desc = param.path("description").asText("");
            String required = param.path("required").asBoolean(false) ? "必填" : "选填";
            params.put(name, String.join(" - ", required, type, desc));
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildResponseExample(JsonNode operation) {
        JsonNode responses = operation.path("responses");
        if (!responses.isObject()) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> iter = responses.fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            if (!entry.getKey().startsWith("2")) {
                continue;
            }
            JsonNode example = entry.getValue().path("content").path("application/json").path("example");
            if (example.isMissingNode()) {
                example = entry.getValue().path("content").path("application/json").path("schema").path("example");
            }
            if (!example.isMissingNode() && !example.isNull()) {
                try {
                    return objectMapper.writeValueAsString(example);
                } catch (Exception e) {
                    return example.asText();
                }
            }
        }
        return null;
    }

    private List<Map<String, Object>> parseParameters(String requestParams) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!StringUtils.hasText(requestParams)) {
            return result;
        }
        try {
            JsonNode node = objectMapper.readTree(requestParams);
            if (!node.isObject()) {
                return result;
            }
            Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                Map<String, Object> param = new LinkedHashMap<>();
                param.put("name", entry.getKey());
                param.put("in", "query");
                param.put("required", false);
                param.put("description", entry.getValue().asText(""));
                param.put("schema", Map.of("type", "string"));
                result.add(param);
            }
        } catch (Exception ignored) {
            // 参数无法解析时导出空列表
        }
        return result;
    }

    private Object parseExample(String responseExample) {
        if (!StringUtils.hasText(responseExample)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(responseExample, Object.class);
        } catch (Exception e) {
            return responseExample;
        }
    }

    private InterfaceGroup findOrCreateGroup(String name) {
        InterfaceGroup group = groupMapper.selectOne(
                new LambdaQueryWrapper<InterfaceGroup>().eq(InterfaceGroup::getName, name));
        if (group != null) {
            return group;
        }
        group = new InterfaceGroup();
        group.setName(name);
        group.setSortOrder(0);
        group.setIsDelete(0);
        groupMapper.insert(group);
        return group;
    }

    private InterfaceTag findOrCreateTag(String name) {
        InterfaceTag tag = tagMapper.selectOne(
                new LambdaQueryWrapper<InterfaceTag>().eq(InterfaceTag::getName, name));
        if (tag != null) {
            return tag;
        }
        tag = new InterfaceTag();
        tag.setName(name);
        tag.setColor("#2563eb");
        tag.setIsDelete(0);
        tagMapper.insert(tag);
        return tag;
    }

    private InterfaceVersion toVersion(InterfaceInfo info) {
        InterfaceVersion version = new InterfaceVersion();
        version.setInterfaceId(info.getId());
        version.setName(info.getName());
        version.setDescription(info.getDescription());
        version.setMethod(info.getMethod());
        version.setUrl(info.getUrl());
        version.setRequestParams(info.getRequestParams());
        version.setResponseExample(info.getResponseExample());
        version.setStatus(info.getStatus());
        return version;
    }
}
