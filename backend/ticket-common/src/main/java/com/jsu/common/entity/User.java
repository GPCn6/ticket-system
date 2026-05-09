package com.jsu.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：sys_user
 *
 * @TableName 映射表名
 * @TableId 主键策略：自增
 * @JsonProperty(access = WRITE_ONLY) 密码字段仅在写入时序列化，响应时隐藏
 */
@TableName("sys_user")
public class User extends Model<User> {

    /** 用户ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一，用于登录 */
    @TableField("username")
    private String username;

    /** 密码，响应时隐藏（WRITE_ONLY） */
    @TableField("password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 手机号，可选，用于绑定/验证 */
    @TableField("phone")
    private String phone;

    /** 邮箱，可选，用于绑定/验证 */
    @TableField("email")
    private String email;

    /** 昵称，用于前端展示 */
    @TableField("nickname")
    private String nickname;

    /** 头像URL */
    @TableField("avatar")
    private String avatar;

    /** 账户状态：1=启用，0=禁用 */
    @TableField("status")
    private Integer status;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** JWT Token，非数据库字段，登录成功后返回 */
    @TableField(exist = false)
    private String token;

    /** 用户角色：user=普通用户，admin=管理员 */
    @TableField("role")
    private String role;

    // ==================== Getter/Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
