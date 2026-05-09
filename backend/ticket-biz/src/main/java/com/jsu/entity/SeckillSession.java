package com.jsu.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀场次实体类
 * 对应数据库表：biz_seckill_session
 *
 * 一个演出可以有多个秒杀场次，每个场次有独立的：
 * - 秒杀时间段（startTime ~ endTime）
 * - 秒杀库存（stock）
 * - 秒杀价格（seckillPrice）
 *
 * @TableName 映射数据库表名
 * @TableId 主键策略：自增
 */
@TableName("biz_seckill_session")
public class SeckillSession {

    /** 场次ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联演出ID */
    @TableField("show_id")
    private Long showId;

    /** 关联票档ID */
    @TableField("ticket_id")
    private Long ticketId;

    /** 秒杀开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 秒杀库存数量 */
    @TableField("stock")
    private Integer stock;

    /** 场次状态（预留字段） */
    @TableField("status")
    private Integer status;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 关联对象（非数据库字段） ====================

    /** 关联演出对象（查询时附加） */
    @TableField(exist = false)
    private Show show;

    /** 关联票档对象（查询时附加） */
    @TableField(exist = false)
    private Ticket ticket;

    // ==================== 业务字段 ====================

    /** 场次名称（可选，用于自定义秒杀场次名称） */
    @TableField("name")
    private String name;

    /** 秒杀价格（必填，通常低于原价） */
    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    // ==================== Getter/Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

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

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
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

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    /**
     * 获取场次名称
     * 优先级：自定义名称 > 关联演出名称
     */
    public String getName() {
        if (this.name != null && !this.name.isEmpty()) {
            return this.name;
        }
        return show != null ? show.getName() : null;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取秒杀价格
     * 优先级：自定义秒杀价 > 票档原价
     */
    public BigDecimal getSeckillPrice() {
        if (this.seckillPrice != null) {
            return this.seckillPrice;
        }
        return ticket != null ? ticket.getPrice() : null;
    }

    public void setSeckillPrice(BigDecimal seckillPrice) {
        this.seckillPrice = seckillPrice;
    }
}

