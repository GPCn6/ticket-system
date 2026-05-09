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
 * 提供用户注册、登录、信息管理等相关接口
 *
 * @RestController 标记为RESTful风格控制器
 * @RequestMapping("/api/user") 路由前缀：/api/user
 * @Tag Swagger文档分组标签
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录接口
     * 使用JWT Token认证，登录成功后返回Token
     *
     * @param request 包含用户名和密码的登录请求体
     * @return 登录成功返回用户信息（含Token），失败抛出业务异常
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<User> login(@RequestBody LoginRequest request) {
        System.out.println("收到登录请求: username=" + request.getUsername() + ", password=" + request.getPassword());
        User user = userService.login(request.getUsername(), request.getPassword());
        return Result.success(user);
    }

    /**
     * 调试用登录接口（测试环境使用）
     */
    @PostMapping("/login-debug")
    @Operation(summary = "调试登录")
    public Result<Map<String, String>> loginDebug(@RequestBody Map<String, String> body) {
        System.out.println("调试登录请求: " + body);
        return Result.success(body);
    }

    /**
     * 用户注册接口
     * 注册时自动分配默认角色为"user"，状态为启用
     *
     * @param user 用户信息（username、password必填，phone/email可选）
     * @return 注册成功返回用户信息（含Token）
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<User> register(@RequestBody User user) {
        user = userService.register(user);
        return Result.success(user);
    }

    /**
     * 获取当前登录用户信息
     * 通过Token验证用户身份
     *
     * @param request HTTP请求，包含Authorization Bearer Token
     * @return 用户详细信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<User> getUserInfo(HttpServletRequest request) {
        String token = extractBearer(request);
        Long userId = userService.verifyToken(token);
        User user = userService.getById(userId);
        return Result.success(user);
    }

    /**
     * 更新当前用户信息
     * 只能更新自己的信息（通过Token验证身份）
     *
     * @param user 更新后的用户信息
     * @param request HTTP请求
     */
    @PutMapping("/update")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateUser(@RequestBody User user, HttpServletRequest request) {
        String token = extractBearer(request);
        Long userId = userService.verifyToken(token);
        user.setId(userId);
        userService.update(user);
        return Result.success();
    }

    /**
     * 重置当前用户密码
     *
     * @param request 包含新密码的请求体
     * @param httpRequest HTTP请求
     */
    @PutMapping("/reset-password")
    @Operation(summary = "重置密码")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String token = extractBearer(httpRequest);
        Long userId = userService.verifyToken(token);
        userService.resetPassword(userId, request.getNewPassword());
        return Result.success();
    }

    /**
     * 用户登出接口
     * 客户端删除Token即可，服务端无需特殊处理
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        return Result.success();
    }

    /**
     * 从请求头中提取Bearer Token
     *
     * @param request HTTP请求
     * @return 提取的Token字符串（不含"Bearer "前缀）
     */
    private static String extractBearer(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
