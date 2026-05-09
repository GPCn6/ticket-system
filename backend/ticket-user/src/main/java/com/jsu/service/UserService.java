package com.jsu.service;

import com.jsu.common.entity.User;

/**
 * 用户服务接口
 * 定义用户相关的业务操作
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回用户信息（包含Token），失败抛出异常
     */
    User login(String username, String password);

    /**
     * 用户注册
     *
     * @param user 注册用户信息
     * @return 注册成功返回用户信息（包含Token）
     */
    User register(User user);

    /**
     * 根据ID获取用户
     */
    User getById(Long id);

    /**
     * 根据用户名获取用户
     */
    User getByUsername(String username);

    /**
     * 根据手机号获取用户
     */
    User getByPhone(String phone);

    /**
     * 更新用户信息
     */
    void update(User user);

    /**
     * 重置密码
     *
     * @param id 用户ID
     * @param newPassword 新密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 生成Token
     *
     * @param userId 用户ID
     * @return JWT Token字符串
     */
    String generateToken(Long userId);

    /**
     * 验证Token
     *
     * @param token JWT Token
     * @return 用户ID
     */
    Long verifyToken(String token);
}
