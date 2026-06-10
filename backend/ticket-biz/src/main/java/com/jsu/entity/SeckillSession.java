package com.jsu.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀场次实体类
 * 对应数据库表：biz_seckill_session
 */
@TableName("biz_seckill_session")
public class SeckillSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("show_id")
    private Long showId;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("stock")
    private Integer stock;

    @TableField("status")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 关联对象（非数据库字段） ====================

    @TableField(exist = false)
    private Show show;

    @TableField(exist = false)
    private Ticket ticket;

    // ==================== 业务字段 ====================

    @TableField("name")
    private String name;

    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    // ==================== 非持久化扩展字段 ====================

    /** 是否已预热到 Redis，前端管理端展示用 */
    @TableField(exist = false)
    private Boolean warmedUp;

    // ==================== Getter/Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public String getName() {
        if (this.name != null && !this.name.isEmpty()) {
            return this.name;
        }
        return show != null ? show.getName() : null;
    }
    public void setName(String name) { this.name = name; }

    public BigDecimal getSeckillPrice() {
        if (this.seckillPrice != null) {
            return this.seckillPrice;
        }
        return ticket != null ? ticket.getPrice() : null;
    }
    public void setSeckillPrice(BigDecimal seckillPrice) { this.seckillPrice = seckillPrice; }

    public Boolean getWarmedUp() { return warmedUp; }
    public void setWarmedUp(Boolean warmedUp) { this.warmedUp = warmedUp; }
}
