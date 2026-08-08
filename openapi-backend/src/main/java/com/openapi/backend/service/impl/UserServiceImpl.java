package com.openapi.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openapi.backend.common.PasswordUtils;
import com.openapi.backend.entity.User;
import com.openapi.backend.mapper.UserMapper;
import com.openapi.backend.service.UserService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
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
        user.setUserPassword(PasswordUtils.encode(userPassword));
        user.setUserName(StringUtils.hasText(userName) ? userName : userAccount);
        user.setUserRole("user");
        user.setIsDelete(0);
        save(user);
        return user;
    }

    @Override
    public User login(String userAccount, String userPassword) {
        User user = lambdaQuery().eq(User::getUserAccount, userAccount).one();
        if (user == null || !passwordMatches(user, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "账号已被禁用");
        }
        migrateLegacyPassword(user, userPassword);
        user.setUserPassword(null);
        return user;
    }

    @Override
    public boolean verifyPassword(Long userId, String rawPassword) {
        User user = getById(userId);
        return user != null && StringUtils.hasText(rawPassword) && passwordMatches(user, rawPassword);
    }

    private boolean passwordMatches(User user, String rawPassword) {
        String stored = user.getUserPassword();
        if (PasswordUtils.isLegacySha256(stored)) {
            return PasswordUtils.sha256(rawPassword).equalsIgnoreCase(stored);
        }
        return PasswordUtils.matches(rawPassword, stored);
    }

    private void migrateLegacyPassword(User user, String rawPassword) {
        if (PasswordUtils.isLegacySha256(user.getUserPassword())) {
            try {
                user.setUserPassword(PasswordUtils.encode(rawPassword));
                updateById(user);
            } catch (Exception e) {
                log.warn("存量密码迁移 BCrypt 失败，账号：{}", user.getUserAccount(), e);
            }
        }
    }

    @Override
    public List<User> listUsers(String keyword) {
        List<User> users = StringUtils.hasText(keyword)
                ? lambdaQuery()
                        .like(User::getUserAccount, keyword)
                        .or()
                        .like(User::getUserName, keyword)
                        .list()
                : list();
        users.forEach(user -> user.setUserPassword(null));
        return users;
    }

    @Override
    public void updateRole(Long id, String role) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        user.setUserRole(role);
        updateById(user);
    }

    @Override
    public void updateStatus(Long id, int status) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        user.setStatus(status);
        updateById(user);
    }

    @Override
    public User createUser(String userAccount, String userPassword, String userName, String role) {
        long count = lambdaQuery().eq(User::getUserAccount, userAccount).count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(PasswordUtils.encode(userPassword));
        user.setUserName(StringUtils.hasText(userName) ? userName : userAccount);
        user.setUserRole(StringUtils.hasText(role) ? role : "user");
        user.setStatus(1);
        user.setIsDelete(0);
        save(user);
        user.setUserPassword(null);
        return user;
    }

    @Override
    public void updateUser(Long id, String userName, String userPassword) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (StringUtils.hasText(userName)) {
            user.setUserName(userName);
        }
        if (StringUtils.hasText(userPassword)) {
            user.setUserPassword(PasswordUtils.encode(userPassword));
        }
        updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        removeById(id);
    }
}
