package com.jsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsu.common.entity.User;

/**
 * 用户 Mapper 接口
 *
 * 提供三种查询方式：用户名、手机号、邮箱
 * 注册时会通过这三个方法检查唯一性
 *
 * 注意：密码存储为明文（当前项目要求），生产环境建议使用 BCrypt 加密
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     * 用于登录验证和注册重名校验
     *
     * @param username 用户名
     * @return 用户信息（包含密码）
     */
    User selectByUsername(String username);

    /**
     * 根据手机号查询用户
     * 用于注册时校验手机号唯一性
     *
     * @param phone 手机号
     * @return 用户信息
     */
    User selectByPhone(String phone);

    /**
     * 根据邮箱查询用户
     * 用于注册时校验邮箱唯一性
     *
     * @param email 邮箱
     * @return 用户信息
     */
    User selectByEmail(String email);
}
