package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.domain.entity.InterfaceTag;

import java.util.List;
import java.util.Map;

public interface InterfaceTagService extends IService<InterfaceTag> {

    List<Map<String, Object>> listWithCounts();

    InterfaceTag createTag(String name);

    void deleteTag(Long id);
}
