package com.jsu.service;

import com.jsu.common.entity.User;
import com.jsu.common.exception.BusinessException;
import com.jsu.common.result.ResultCode;

/**
 * 用户服务接口
 */
public interface UserService {

    User login(String username, String password);

    User register(User user);

    User getById(Long id);

    User getByUsername(String username);

    User getByPhone(String phone);

    void update(User user);

    void resetPassword(Long id, String newPassword);

    String generateToken(Long userId);

    /** 登出时将 token 加入黑名单 */
    void invalidateToken(String token);

    Long verifyToken(String token);
}
