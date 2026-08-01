package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.backend.common.PasswordUtils;
import com.openapi.backend.entity.User;
import com.openapi.backend.mapper.UserMapper;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User register(String userAccount, String userPassword, String userName) {
        long count = lambdaQuery().eq(User::getUserAccount, userAccount).count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(PasswordUtils.sha256(userPassword));
        user.setUserName(StringUtils.hasText(userName) ? userName : userAccount);
        user.setUserRole("user");
        user.setIsDelete(0);
        save(user);
        return user;
    }

    @Override
    public User login(String userAccount, String userPassword) {
        User user = lambdaQuery().eq(User::getUserAccount, userAccount).one();
        if (user == null || !user.getUserPassword().equals(PasswordUtils.sha256(userPassword))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        user.setUserPassword(null);
        return user;
    }
}
