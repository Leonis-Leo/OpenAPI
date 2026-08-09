package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.InterfaceVersion;

import java.util.List;

public interface InterfaceVersionService extends IService<InterfaceVersion> {

    /** 生成下一个版本快照，返回新版本 */
    InterfaceVersion snapshot(InterfaceVersion source, String changeNote, Long createBy);

    /** 按接口查询版本列表（倒序） */
    List<InterfaceVersion> listByInterface(Long interfaceId);

    /** 获取指定接口的指定版本 */
    InterfaceVersion getVersion(Long interfaceId, Long versionId);
}
