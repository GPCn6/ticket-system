package com.jsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsu.entity.Ticket;

import java.util.List;

/**
 * 票档 Mapper 接口
 *
 * 核心功能：库存扣减和恢复（使用乐观锁）
 * - deductStock 的 SQL 中带有 WHERE available_stock >= #{quantity} 条件
 *   在更新时检查库存是否充足，防止超卖
 * - restoreStock 直接增加库存（订单取消时使用）
 */
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 获取指定演出的所有票档
     * 按价格正序排列
     *
     * @param showId 演出ID
     * @return 票档列表
     */
    List<Ticket> selectByShowId(Long showId);

    /**
     * 扣减票档库存（乐观锁）
     * SQL: UPDATE biz_ticket SET available_stock = available_stock - #{quantity}
     *      WHERE id = #{ticketId} AND available_stock >= #{quantity}
     *
     * @param ticketId 票档ID
     * @param quantity 扣减数量
     * @return 影响行数（0表示库存不足或更新失败）
     */
    int deductStock(Long ticketId, Integer quantity);

    /**
     * 恢复票档库存
     * 订单取消时使用
     *
     * @param ticketId 票档ID
     * @param quantity 恢复数量
     * @return 影响行数
     */
    int restoreStock(Long ticketId, int quantity);
}
