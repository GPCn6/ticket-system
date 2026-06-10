package com.jsu.service;

import com.jsu.common.result.Result;
import com.jsu.entity.SeckillSession;

import java.util.List;

/**
 * 秒杀服务接口
 */
public interface SeckillService {

    /**
     * 预热秒杀库存到Redis
     * @param sessionId 秒杀场次ID
     * @return 实际预热库存数
     */
    int warmUpStock(Long sessionId);

    /**
     * 批量预热所有即将开始的秒杀场次
     * @return 成功预热的场次数
     */
    int batchWarmUp();

    /**
     * 执行秒杀
     */
    Result<?> seckill(Long sessionId, Long userId, int quantity);

    List<SeckillSession> getActiveSessions();
    List<SeckillSession> getUpcomingSessions();
    List<SeckillSession> getAllSessions();
    SeckillSession getById(Long id);
    void create(SeckillSession session);
    void update(SeckillSession session);
    void delete(Long id);
}
