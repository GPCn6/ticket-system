package com.jsu.service.Impl;

import com.jsu.common.entity.User;
import com.jsu.common.exception.BusinessException;
import com.jsu.common.result.ResultCode;
import com.jsu.common.util.StringUtils;
import com.jsu.mapper.UserMapper;
import com.jsu.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 用户服务实现类
 * 处理用户登录、注册、Token生成与验证等核心业务逻辑
 */
@Service
public class UserServiceImpl implements UserService {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** Token 黑名单 Key 前缀 */
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Value("${jwt.secret:TicketSystemSecretKey2026ForJWTTokenGeneration}")
    private String jwtSecret;

    @Value("${jwt.expiration:7200000}")
    private long jwtExpiration;

    public UserServiceImpl(UserMapper userMapper, StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        String token = generateToken(user.getId(), user.getRole());
        user.setToken(token);
        return user;
    }

    @Override
    @Transactional
    public User register(User user) {
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (user.getPhone() != null && userMapper.selectByPhone(user.getPhone()) != null) {
            throw new BusinessException("手机号已被注册");
        }
        if (user.getEmail() != null && userMapper.selectByEmail(user.getEmail()) != null) {
            throw new BusinessException("邮箱已被注册");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) user.setStatus(1);
        if (user.getNickname() == null) user.setNickname(user.getUsername());
        if (user.getRole() == null) user.setRole("user");
        userMapper.insert(user);
        user.setToken(generateToken(user.getId(), user.getRole()));
        return user;
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public User getByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }

    @Override
    @Transactional
    public void update(User user) {
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public String generateToken(Long userId) {
        User user = userMapper.selectById(userId);
        return generateToken(userId, user != null ? user.getRole() : "user");
    }

    /**
     * 生成 JWT Token，并缓存 tokenId
     * Token 包含：tokenId（支持登出失效）、userId（subject）、role
     */
    private String generateToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtExpiration, ChronoUnit.MILLIS);

        // 生成唯一 tokenId，用于登出黑名单
        String tokenId = StringUtils.generateUUID();

        String token = Jwts.builder()
                .id(tokenId)                                    // jti：Token 唯一 ID，用于黑名单
                .subject(userId.toString())                     // 用户ID
                .claim("role", role == null ? "user" : role)    // 用户角色
                .claim("tokenId", tokenId)                      // Token ID（冗余，便于Gateway读取）
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();

        return token;
    }

    /**
     * 登出时标记 tokenId 为黑名单
     * 添加后 Gateway 的 AuthFilter 会拒绝该 Token 的后续请求
     */
    @Override
    public void invalidateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String tokenId = claims.getId(); // JWT jti claim
            if (tokenId != null) {
                // 计算剩余有效期作为黑名单 TTL
                Date expiration = claims.getExpiration();
                long remainingTtl = expiration.getTime() - System.currentTimeMillis();
                if (remainingTtl > 0) {
                    stringRedisTemplate.opsForValue().set(
                            TOKEN_BLACKLIST_PREFIX + tokenId,
                            "1",
                            Duration.ofMillis(remainingTtl));
                }
            }
        } catch (Exception e) {
            // Token 已过期或不合法，无需处理
        }
    }

    @Override
    public Long verifyToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            return Long.parseLong(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
    }
}
