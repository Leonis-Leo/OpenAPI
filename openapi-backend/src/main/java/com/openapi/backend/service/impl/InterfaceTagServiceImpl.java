package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.domain.entity.InterfaceTag;
import com.openapi.domain.entity.InterfaceTagRelation;
import com.openapi.domain.mapper.InterfaceTagMapper;
import com.openapi.domain.mapper.InterfaceTagRelationMapper;
import com.openapi.backend.service.InterfaceTagService;
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
public class InterfaceTagServiceImpl extends ServiceImpl<InterfaceTagMapper, InterfaceTag>
        implements InterfaceTagService {

    private final InterfaceTagRelationMapper tagRelationMapper;

    @Override
    public List<Map<String, Object>> listWithCounts() {
        return lambdaQuery()
                .orderByAsc(InterfaceTag::getId)
                .list()
                .stream()
                .map(tag -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", tag.getId());
                    map.put("name", tag.getName());
                    map.put("color", tag.getColor());
                    Long count = tagRelationMapper.selectCount(
                            new LambdaQueryWrapper<InterfaceTagRelation>().eq(InterfaceTagRelation::getTagId, tag.getId()));
                    map.put("interfaceCount", count == null ? 0 : count);
                    return map;
                })
                .toList();
    }

    @Override
    public InterfaceTag createTag(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称不能为空");
        }
        Long exists = lambdaQuery().eq(InterfaceTag::getName, name.trim()).count();
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签已存在");
        }
        InterfaceTag tag = new InterfaceTag();
        tag.setName(name.trim());
        tag.setColor("#2563eb");
        tag.setIsDelete(0);
        save(tag);
        return tag;
    }

    @Override
    public void deleteTag(Long id) {
        InterfaceTag tag = getById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不存在");
        }
        tagRelationMapper.delete(new LambdaQueryWrapper<InterfaceTagRelation>()
                .eq(InterfaceTagRelation::getTagId, id));
        removeById(id);
    }
}
