package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.InterfaceInfo;

import java.util.List;
import java.util.Map;

public interface InterfaceInfoService extends IService<InterfaceInfo> {

    List<InterfaceInfo> listOnline();

    List<InterfaceInfo> listAll();

    void online(Long id, Long operatorId);

    void offline(Long id);

    /** 更新接口；若当前为已上线状态则生成版本快照 */
    void updateWithVersion(InterfaceInfo info, String tagIds, String changeNote, Long operatorId);

    /** 一键回滚到指定版本，并生成回滚快照 */
    void rollback(Long interfaceId, Long versionId, Long operatorId);

    /** 按标签查询接口 ID 列表 */
    List<Long> interfaceIdsByTag(Long tagId);

    /** 富化接口的 groupName / tags 展示字段 */
    InterfaceInfo enrich(InterfaceInfo info);

    /** 导入 OpenAPI JSON/YAML，返回 {created, skipped} */
    Map<String, Object> openapiImport(String spec, Long operatorId);

    /** 导出 OpenAPI 文档（json 或 yaml） */
    String openapiExport(String format);
}
