package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.domain.entity.InterfaceGroup;
import com.openapi.domain.entity.InterfaceInfo;
import com.openapi.domain.mapper.InterfaceGroupMapper;
import com.openapi.domain.mapper.InterfaceInfoMapper;
import com.openapi.backend.service.InterfaceGroupService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterfaceGroupServiceImpl extends ServiceImpl<InterfaceGroupMapper, InterfaceGroup>
        implements InterfaceGroupService {

    private final InterfaceInfoMapper interfaceInfoMapper;

    @Override
    public List<Map<String, Object>> listWithCounts() {
        return lambdaQuery()
                .orderByAsc(InterfaceGroup::getSortOrder)
                .orderByAsc(InterfaceGroup::getId)
                .list()
                .stream()
                .map(group -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", group.getId());
                    map.put("name", group.getName());
                    map.put("parentId", group.getParentId());
                    map.put("sortOrder", group.getSortOrder());
                    Long count = interfaceInfoMapper.selectCount(
                            new LambdaQueryWrapper<InterfaceInfo>().eq(InterfaceInfo::getGroupId, group.getId()));
                    map.put("interfaceCount", count == null ? 0 : count);
                    if (group.getParentId() != null) {
                        InterfaceGroup parent = getById(group.getParentId());
                        map.put("parentName", parent == null ? null : parent.getName());
                    }
                    return map;
                })
                .toList();
    }

    @Override
    public Map<String, Object> groupTree() {
        List<Map<String, Object>> nodes = listWithCounts();
        Map<Long, List<Map<String, Object>>> childrenMap = new HashMap<>();
        for (Map<String, Object> node : nodes) {
            Object parentId = node.get("parentId");
            if (parentId != null) {
                childrenMap.computeIfAbsent(Long.valueOf(parentId.toString()), key -> new java.util.ArrayList<>())
                        .add(node);
            }
        }
        List<Map<String, Object>> tree = nodes.stream()
                .filter(node -> node.get("parentId") == null)
                .map(node -> {
                    node.put("children", childrenMap.getOrDefault(
                            Long.valueOf(node.get("id").toString()), java.util.List.of()));
                    return node;
                })
                .toList();
        Long total = interfaceInfoMapper.selectCount(null);
        long groupSum = nodes.stream().mapToLong(g -> ((Number) g.get("interfaceCount")).longValue()).sum();
        Map<String, Object> result = new HashMap<>();
        result.put("tree", tree);
        result.put("total", total == null ? 0 : total);
        result.put("ungrouped", Math.max(0, (total == null ? 0 : total) - groupSum));
        return result;
    }

    @Override
    public void createGroup(String name, Long parentId) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分组名称不能为空");
        }
        if (parentId != null && getById(parentId) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父分组不存在");
        }
        Long exists = lambdaQuery().eq(InterfaceGroup::getName, name.trim()).count();
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分组名称已存在");
        }
        InterfaceGroup group = new InterfaceGroup();
        group.setName(name.trim());
        group.setParentId(parentId);
        group.setSortOrder(0);
        group.setIsDelete(0);
        save(group);
    }

    @Override
    public void updateGroup(Long id, String name) {
        InterfaceGroup group = getById(id);
        if (group == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分组不存在");
        }
        if (StringUtils.hasText(name) && !name.trim().equals(group.getName())) {
            Long exists = lambdaQuery().eq(InterfaceGroup::getName, name.trim()).ne(InterfaceGroup::getId, id).count();
            if (exists != null && exists > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分组名称已存在");
            }
            group.setName(name.trim());
            updateById(group);
        }
    }

    @Override
    public void deleteGroup(Long id) {
        InterfaceGroup group = getById(id);
        if (group == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分组不存在");
        }
        Long children = lambdaQuery().eq(InterfaceGroup::getParentId, id).count();
        if (children != null && children > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分组下仍有 " + children + " 个子分组，请先处理");
        }
        Long used = interfaceInfoMapper.selectCount(
                new LambdaQueryWrapper<InterfaceInfo>().eq(InterfaceInfo::getGroupId, id));
        if (used != null && used > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分组下仍有 " + used + " 个接口，请先移出");
        }
        removeById(id);
    }
}
