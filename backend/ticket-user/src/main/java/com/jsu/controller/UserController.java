package com.jsu.controller;

import com.jsu.common.entity.User;
import com.jsu.common.result.Result;
import com.jsu.dto.LoginRequest;
import com.jsu.dto.ResetPasswordRequest;
import com.jsu.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理Controller
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<User> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        return Result.success(user);
    }

    @PostMapping("/login-debug")
    @Operation(summary = "调试登录")
    public Result<Map<String, String>> loginDebug(@RequestBody Map<String, String> body) {
        return Result.success(body);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<User> register(@RequestBody User user) {
        user = userService.register(user);
        return Result.success(user);
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<User> getUserInfo(HttpServletRequest request) {
        String token = extractBearer(request);
        Long userId = userService.verifyToken(token);
        User user = userService.getById(userId);
        return Result.success(user);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateUser(@RequestBody User user, HttpServletRequest request) {
        String token = extractBearer(request);
        Long userId = userService.verifyToken(token);
        user.setId(userId);
        userService.update(user);
        return Result.success();
    }

    @PutMapping("/reset-password")
    @Operation(summary = "重置密码")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String token = extractBearer(httpRequest);
        Long userId = userService.verifyToken(token);
        userService.resetPassword(userId, request.getNewPassword());
        // 重置密码后强制登出：使当前 Token 失效
        userService.invalidateToken(token);
        return Result.success();
    }

    /**
     * 登出接口：将当前 Token 加入 Redis 黑名单
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractBearer(request);
        if (token != null) {
            userService.invalidateToken(token);
        }
        return Result.success();
    }

    private static String extractBearer(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
