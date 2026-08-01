package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.App;

import java.util.List;

public interface AppService extends IService<App> {

    App createApp(String appName, Long userId);

    List<App> listByUserId(Long userId);
}
