package com.jsu.dto;

/**
 * 登录请求DTO
 * 用于接收用户登录接口的请求参数
 */
public class LoginRequest {

    /** 用户名 */
    private String username;

    /** 密码（明文） */
    private String password;

    public LoginRequest() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
