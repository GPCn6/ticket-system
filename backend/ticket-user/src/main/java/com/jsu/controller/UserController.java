package com.jsu.controller;

import com.jsu.common.entity.User;
import com.jsu.common.result.Result;
import com.jsu.dto.LoginRequest;
import com.jsu.dto.RegisterRequest;
import com.jsu.dto.ResetPasswordRequest;
import com.jsu.dto.UpdateUserRequest;
import com.jsu.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

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
        return Result.success(userService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        return Result.success(userService.register(user));
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = userService.verifyToken(extractBearer(request));
        return Result.success(userService.getById(userId));
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateUser(@RequestBody UpdateUserRequest request, HttpServletRequest httpRequest) {
        Long userId = userService.verifyToken(extractBearer(httpRequest));
        User user = new User();
        user.setId(userId);
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setAvatar(request.avatar());
        userService.update(user);
        return Result.success();
    }

    @PutMapping("/reset-password")
    @Operation(summary = "重置密码")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String token = extractBearer(httpRequest);
        Long userId = userService.verifyToken(token);
        userService.resetPassword(userId, request.getNewPassword());
        userService.invalidateToken(token);
        return Result.success();
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractBearer(request);
        if (token != null) userService.invalidateToken(token);
        return Result.success();
    }

    private static String extractBearer(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        return token != null && token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
