package com.jsu.service.Impl;

import com.jsu.common.entity.User;
import com.jsu.common.exception.BusinessException;
import com.jsu.common.result.ResultCode;
import com.jsu.mapper.UserMapper;
import com.jsu.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 用户服务实现类
 * 处理用户登录、注册、Token生成与验证等核心业务逻辑
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    /**
     * JWT加密密钥，配置优先级：配置文件 > 默认值
     * 默认密钥用于本地开发和测试环境
     */
    @Value("${jwt.secret:TicketSystemSecretKey2026ForJWTTokenGeneration}")
    private String jwtSecret;

    /**
     * JWT Token过期时间（毫秒），默认2小时（7200000ms）
     */
    @Value("${jwt.expiration:7200000}")
    private long jwtExpiration;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户登录
     * 1. 根据用户名查询用户
     * 2. 校验密码
     * 3. 生成JWT Token并返回
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 用户信息（包含生成的Token）
     * @throws BusinessException 用户不存在或密码错误
     */
    @Override
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (!password.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        String token = generateToken(user.getId(), user.getRole());
        user.setToken(token);
        return user;
    }

    /**
     * 用户注册
     * 1. 检查用户名、手机号、邮箱唯一性
     * 2. 设置默认角色为"user"
     * 3. 插入数据库并生成Token
     *
     * @param user 注册用户信息
     * @return 注册后的用户信息（包含Token）
     * @throws BusinessException 用户名/手机号/邮箱已被注册
     */
    @Override
    @Transactional
    public User register(User user) {
        // 检查用户名唯一性
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        // 检查手机号唯一性（可选字段）
        if (user.getPhone() != null && userMapper.selectByPhone(user.getPhone()) != null) {
            throw new BusinessException("手机号已被注册");
        }
        // 检查邮箱唯一性（可选字段）
        if (user.getEmail() != null && userMapper.selectByEmail(user.getEmail()) != null) {
            throw new BusinessException("邮箱已被注册");
        }
        user.setPassword(user.getPassword());
        // 设置默认状态为启用
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        // 设置默认昵称为用户名
        if (user.getNickname() == null) {
            user.setNickname(user.getUsername());
        }
        // 设置默认角色为普通用户
        if (user.getRole() == null) {
            user.setRole("user");
        }
        userMapper.insert(user);
        // 注册成功后自动登录，生成Token
        user.setToken(generateToken(user.getId(), user.getRole()));
        return user;
    }

    /**
     * 根据ID获取用户信息
     */
    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据用户名获取用户信息
     */
    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    /**
     * 根据手机号获取用户信息
     */
    @Override
    public User getByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }

    /**
     * 更新用户信息
     */
    @Override
    @Transactional
    public void update(User user) {
        userMapper.updateById(user);
    }

    /**
     * 重置用户密码
     *
     * @param id 用户ID
     * @param newPassword 新密码（明文存储）
     */
    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 按当前项目要求使用明文密码存储
        user.setPassword(newPassword);
        userMapper.updateById(user);
    }

    /**
     * 根据用户ID生成Token
     *
     * @param userId 用户ID
     * @return JWT Token字符串
     */
    @Override
    public String generateToken(Long userId) {
        User user = userMapper.selectById(userId);
        return generateToken(userId, user != null ? user.getRole() : "user");
    }

    /**
     * 生成JWT Token
     * Token包含用户ID和角色信息，过期时间可配置
     *
     * @param userId 用户ID（作为Token的subject）
     * @param role 用户角色（user/admin）
     * @return JWT Token字符串
     */
    private String generateToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Instant now = Instant.now();
        // 计算过期时间：当前时间 + 配置的过期时长
        Instant expiration = now.plus(jwtExpiration, ChronoUnit.MILLIS);
        return Jwts.builder()
                .subject(userId.toString())              // Token主体：用户ID
                .claim("role", role == null ? "user" : role)  // 附加信息：用户角色
                .issuedAt(Date.from(now))                // 签发时间
                .expiration(Date.from(expiration))     // 过期时间
                .signWith(key)                          // 使用HMAC-SHA密钥签名
                .compact();                             // 生成Token字符串
    }

    /**
     * 验证Token并提取用户ID
     *
     * @param token JWT Token字符串
     * @return 用户ID
     * @throws BusinessException Token无效或已过期
     */
    @Override
    public Long verifyToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            // 解析Token，验证签名，获取payload
            return Long.parseLong(Jwts.parser()
                    .verifyWith(key)                    // 设置验证密钥
                    .build()
                    .parseSignedClaims(token)           // 解析并验证签名
                    .getPayload()
                    .getSubject());                     // 提取用户ID
        } catch (Exception e) {
            // Token解析失败（过期、签名错误、格式错误等）
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
    }
}
