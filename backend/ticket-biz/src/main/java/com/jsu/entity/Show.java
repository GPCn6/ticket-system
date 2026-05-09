package com.jsu.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 演出实体类
 * 对应数据库表：biz_show
 *
 * @Data Lombok注解，自动生成getter/setter/toString等
 * @TableName 映射数据库表名
 * @TableId 主键策略：自增
 */
@Data
@TableName("biz_show")
public class Show {

    /** 演出ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 演出名称 */
    private String name;

    /** 海报图片URL */
    private String poster;

    /** 演出描述 */
    private String description;

    /** 演出场馆 */
    private String venue;

    /** 演出城市 */
    private String city;

    /** 演出分类：演唱会、话剧、音乐剧、舞蹈等 */
    private String category;

    /** 演出开始时间 */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 演出结束时间 */
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 演出状态：1=上架/销售中，0=下架，参考ShowStatus枚举 */
    private Integer status;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间，自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // ==================== Getter/Setter ====================

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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
}
