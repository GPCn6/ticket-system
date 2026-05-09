package com.jsu.service;

import com.jsu.entity.Ticket;

import java.util.List;

/**
 * 票档服务接口
 * 定义票档相关的业务操作
 */
public interface TicketService {

    /**
     * 根据ID获取票档
     */
    Ticket getById(Long id);

    /**
     * 获取指定演出的所有票档
     *
     * @param showId 演出ID
     * @return 票档列表
     */
    List<Ticket> getByShowId(Long showId);

    /**
     * 扣减票档库存
     * 使用乐观锁防止超卖
     *
     * @param ticketId 票档ID
     * @param quantity 扣减数量
     * @return true=扣减成功，false=库存不足
     */
    boolean deductStock(Long ticketId, int quantity);

    /**
     * 恢复票档库存
     * 用于订单取消时归还库存
     *
     * @param ticketId 票档ID
     * @param quantity 恢复数量
     * @return true=恢复成功
     */
    boolean restoreStock(Long ticketId, int quantity);

    /**
     * 创建票档
     */
    void create(Ticket ticket);

    /**
     * 更新票档
     */
    void update(Ticket ticket);

    /**
     * 删除票档
     */
    void delete(Long id);
}