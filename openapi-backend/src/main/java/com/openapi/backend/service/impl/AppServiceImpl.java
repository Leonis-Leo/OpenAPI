package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.backend.entity.App;
import com.openapi.backend.mapper.AppMapper;
import com.openapi.backend.service.AppService;
import com.openapi.common.utils.KeyGeneratorUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Override
    public App createApp(String appName, Long userId) {
        App app = new App();
        app.setAppName(appName);
        app.setUserId(userId);
        app.setAccessKey("AK" + KeyGeneratorUtils.generateKey(16));
        app.setSecretKey("SK" + KeyGeneratorUtils.generateKey(32));
        app.setStatus(1);
        save(app);
        return app;
    }

    @Override
    public List<App> listByUserId(Long userId) {
        return lambdaQuery().eq(App::getUserId, userId).list();
    }
}
