package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.InterfaceGroup;

import java.util.List;
import java.util.Map;

public interface InterfaceGroupService extends IService<InterfaceGroup> {

    List<Map<String, Object>> listWithCounts();

    Map<String, Object> groupTree();

    void createGroup(String name, Long parentId);

    void updateGroup(Long id, String name);

    void deleteGroup(Long id);
}
