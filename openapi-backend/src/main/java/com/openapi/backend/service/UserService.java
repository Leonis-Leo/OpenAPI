package com.openapi.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openapi.backend.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    User register(String userAccount, String userPassword, String userName);

    User login(String userAccount, String userPassword);

    List<User> listUsers(String keyword);

    void updateRole(Long id, String role);

    void updateStatus(Long id, int status);

    User createUser(String userAccount, String userPassword, String userName, String role);

    void updateUser(Long id, String userName, String userPassword);

    void deleteUser(Long id);
}
