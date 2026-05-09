package com.jsu.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票档实体类
 * 对应数据库表：biz_ticket
 *
 * 票档是演出的子集，一个演出可以有多个票档（如：VIP票、普通票、学生票）
 *
 * @TableName 映射数据库表名
 * @TableId 主键策略：自增
 */
@TableName("biz_ticket")
public class Ticket {

    /** 票档ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联演出ID */
    @TableField("show_id")
    private Long showId;

    /** 票档名称，如：VIP票、普通票、学生票 */
    @TableField("name")
    private String name;

    /** 票档单价 */
    @TableField("price")
    private BigDecimal price;

    /** 总库存数量 */
    @TableField("total_stock")
    private Integer totalStock;

    /** 可用库存数量（实时扣减） */
    @TableField("available_stock")
    private Integer availableStock;

    /** 座位范围描述，如："A区、B区" */
    @TableField("seat_range")
    private String seatRange;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public String getSeatRange() {
        return seatRange;
    }

    public void setSeatRange(String seatRange) {
        this.seatRange = seatRange;
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
}

