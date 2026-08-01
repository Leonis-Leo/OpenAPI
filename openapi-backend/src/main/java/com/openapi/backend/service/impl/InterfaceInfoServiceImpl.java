package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.mapper.InterfaceInfoMapper;
import com.openapi.backend.service.InterfaceInfoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
        implements InterfaceInfoService {

    @Override
    public List<InterfaceInfo> listOnline() {
        return lambdaQuery().eq(InterfaceInfo::getStatus, 1).list();
    }
}
