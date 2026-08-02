package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.InterfaceInfo;

import java.util.List;

public interface InterfaceInfoService extends IService<InterfaceInfo> {

    List<InterfaceInfo> listOnline();

    void online(Long id);

    void offline(Long id);
}
