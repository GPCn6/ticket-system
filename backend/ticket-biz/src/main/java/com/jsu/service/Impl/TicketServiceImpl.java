package com.jsu.service.Impl;

import com.jsu.entity.Ticket;
import com.jsu.mapper.TicketMapper;
import com.jsu.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 票档服务实现类
 * 处理票档的查询、库存管理等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;

    /**
     * 根据ID获取票档
     */
    @Override
    public Ticket getById(Long id) {
        return ticketMapper.selectById(id);
    }

    /**
     * 获取指定演出的所有票档
     *
     * @param showId 演出ID
     * @return 票档列表
     */
    @Override
    public List<Ticket> getByShowId(Long showId) {
        return ticketMapper.selectByShowId(showId);
    }

    /**
     * 扣减票档库存
     * 使用乐观锁防止超卖
     *
     * @param ticketId 票档ID
     * @param quantity 扣减数量
     * @return true=扣减成功，false=库存不足或更新失败
     */
    @Override
    @Transactional
    public boolean deductStock(Long ticketId, int quantity) {
        if (ticketId == null || quantity <= 0) return false;
        int updated = ticketMapper.deductStock(ticketId, quantity);
        return updated > 0;
    }

    /**
     * 恢复票档库存
     * 用于订单取消时归还库存
     *
     * @param ticketId 票档ID
     * @param quantity 恢复数量
     * @return true=恢复成功
     */
    @Override
    @Transactional
    public boolean restoreStock(Long ticketId, int quantity) {
        if (ticketId == null || quantity <= 0) return false;
        int updated = ticketMapper.restoreStock(ticketId, quantity);
        return updated > 0;
    }

    /**
     * 创建票档
     */
    @Override
    @Transactional
    public void create(Ticket ticket) {
        ticketMapper.insert(ticket);
    }

    /**
     * 更新票档
     */
    @Override
    @Transactional
    public void update(Ticket ticket) {
        ticketMapper.updateById(ticket);
    }

    /**
     * 删除票档
     */
    @Override
    @Transactional
    public void delete(Long id) {
        ticketMapper.deleteById(id);
    }
}
