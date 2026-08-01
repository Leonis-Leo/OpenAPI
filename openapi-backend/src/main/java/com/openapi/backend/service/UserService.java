package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.User;

public interface UserService extends IService<User> {

    User register(String userAccount, String userPassword, String userName);

    User login(String userAccount, String userPassword);
}
