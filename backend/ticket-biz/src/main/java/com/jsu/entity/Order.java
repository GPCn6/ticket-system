package com.jsu.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jsu.common.constant.OrderStatus;
import com.jsu.common.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 对应数据库表：biz_order
 *
 * @TableName 映射数据库表名
 * @TableId 主键策略：自增
 */
@TableName("biz_order")
public class Order {

    /** 订单ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，唯一，用于对外展示和查询 */
    @TableField("order_no")
    private String orderNo;

    /** 下单用户ID */
    @TableField("user_id")
    private Long userId;

    /** 关联演出ID */
    @TableField("show_id")
    private Long showId;

    /** 关联票档ID */
    @TableField("ticket_id")
    private Long ticketId;

    /** 关联座位ID（可选，指定座位时使用） */
    @TableField("seat_id")
    private Long seatId;

    /** 购买数量 */
    @TableField("quantity")
    private Integer quantity;

    /** 订单总金额 */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /** 订单状态：0=待支付，1=已支付，2=已取消，3=已退款，4=已完成 */
    @TableField("status")
    private Integer status;

    /** 支付时间 */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /** 订单过期时间（创建后15分钟） */
    @TableField("expire_time")
    private LocalDateTime expireTime;

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

    /** 关联座位对象（查询时附加） */
    @TableField(exist = false)
    private Seat seat;

    /** 关联用户对象（查询时附加） */
    @TableField(exist = false)
    private User user;

    // ==================== 扩展字段（非数据库字段，用于前端展示） ====================

    /** 演出名称（冗余字段，避免关联查询） */
    @TableField(exist = false)
    private String showName;

    /** 票档名称（冗余字段） */
    @TableField(exist = false)
    private String ticketName;

    /** 用户名称（冗余字段） */
    @TableField(exist = false)
    private String userName;

    /** 订单总价（同totalAmount，用于兼容） */
    @TableField(exist = false)
    private BigDecimal totalPrice;

    /** 秒杀订单的秒杀价格（非秒杀订单为null） */
    @TableField(exist = false)
    private BigDecimal seckillPrice;

    // ==================== 秒杀相关字段 ====================

    /** 是否秒杀订单：1=秒杀订单，0=普通订单 */
    @TableField("is_seckill")
    private Integer isSeckill;

    /** 秒杀场次ID（普通订单为null） */
    @TableField("session_id")
    private Long sessionId;

    // ==================== 计算属性 ====================

    /**
     * 获取演出名称（优先返回冗余字段，否则从关联对象获取）
     */
    public String getShowName() {
        if (showName != null && !showName.isEmpty()) {
            return showName;
        }
        return show != null ? show.getName() : null;
    }

    /**
     * 获取票档名称
     */
    public String getTicketName() {
        if (ticketName != null && !ticketName.isEmpty()) {
            return ticketName;
        }
        return ticket != null ? ticket.getName() : null;
    }

    /**
     * 获取订单总价
     */
    public BigDecimal getTotalPrice() {
        if (totalPrice != null) {
            return totalPrice;
        }
        return totalAmount;
    }

    /**
     * 获取秒杀价格
     */
    public BigDecimal getSeckillPrice() {
        return seckillPrice;
    }

    public void setSeckillPrice(BigDecimal seckillPrice) {
        this.seckillPrice = seckillPrice;
    }

    /**
     * 获取订单状态名称
     * @return 状态中文名称，如"待支付"、"已支付"等
     */
    public String getStatusName() {
        OrderStatus s = OrderStatus.getByCode(this.status == null ? -1 : this.status);
        return s != null ? s.getName() : "未知";
    }

    /**
     * 获取用户名
     */
    public String getUserName() {
        if (userName != null && !userName.isEmpty()) {
            return userName;
        }
        return user != null ? user.getUsername() : null;
    }

    // ==================== Getter/Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JsonProperty("isSeckill")
    public Integer getIsSeckill() {
        return isSeckill;
    }

    public void setIsSeckill(Integer isSeckill) {
        this.isSeckill = isSeckill;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}
