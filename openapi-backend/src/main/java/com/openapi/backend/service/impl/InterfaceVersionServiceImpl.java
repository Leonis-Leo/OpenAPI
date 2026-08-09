package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.backend.entity.InterfaceVersion;
import com.openapi.backend.mapper.InterfaceVersionMapper;
import com.openapi.backend.service.InterfaceVersionService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterfaceVersionServiceImpl extends ServiceImpl<InterfaceVersionMapper, InterfaceVersion>
        implements InterfaceVersionService {

    @Override
    public InterfaceVersion snapshot(InterfaceVersion source, String changeNote, Long createBy) {
        int nextNo = 1;
        InterfaceVersion latest = lambdaQuery()
                .eq(InterfaceVersion::getInterfaceId, source.getInterfaceId())
                .orderByDesc(InterfaceVersion::getVersionNo)
                .last("LIMIT 1")
                .one();
        if (latest != null && latest.getVersionNo() != null) {
            nextNo = latest.getVersionNo() + 1;
        }
        InterfaceVersion version = new InterfaceVersion();
        version.setInterfaceId(source.getInterfaceId());
        version.setVersionNo(nextNo);
        version.setName(source.getName());
        version.setDescription(source.getDescription());
        version.setMethod(source.getMethod());
        version.setUrl(source.getUrl());
        version.setRequestParams(source.getRequestParams());
        version.setResponseExample(source.getResponseExample());
        version.setStatus(source.getStatus());
        version.setChangeNote(changeNote);
        version.setCreateBy(createBy);
        version.setIsDelete(0);
        save(version);
        return version;
    }

    @Override
    public List<InterfaceVersion> listByInterface(Long interfaceId) {
        return lambdaQuery()
                .eq(InterfaceVersion::getInterfaceId, interfaceId)
                .orderByDesc(InterfaceVersion::getVersionNo)
                .list();
    }

    @Override
    public InterfaceVersion getVersion(Long interfaceId, Long versionId) {
        InterfaceVersion version = getById(versionId);
        if (version == null || !version.getInterfaceId().equals(interfaceId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本不存在");
        }
        return version;
    }
}
