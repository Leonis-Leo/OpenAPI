package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.service.InterfaceInfoService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
        implements InterfaceInfoService {

    @Override
    public List<InterfaceInfo> listOnline() {
        return lambdaQuery().eq(InterfaceInfo::getStatus, 1).list();
    }

    @Override
    public void online(Long id) {
        updateStatus(id, 1);
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
}
