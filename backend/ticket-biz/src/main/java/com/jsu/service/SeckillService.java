package com.jsu.service;

import com.jsu.common.result.Result;
import com.jsu.entity.SeckillSession;

import java.util.List;

/**
 * 秒杀服务接口
 * 定义秒杀相关的业务操作
 *
 * 技术方案：
 * 1. Redis预热库存 → 提高并发处理能力
 * 2. Lua脚本原子操作 → 保证库存扣减的原子性
 * 3. 分布式锁 → 防止重复抢购
 * 4. MQ异步处理 → 削峰填谷
 */
public interface SeckillService {

    /**
     * 预热秒杀库存到Redis
     * 将数据库中的库存同步到Redis缓存
     *
     * @param sessionId 秒杀场次ID
     */
    void warmUpStock(Long sessionId);

    /**
     * 执行秒杀
     *
     * @param sessionId 秒杀场次ID
     * @param userId 用户ID
     * @param quantity 购买数量
     * @return 秒杀结果
     */
    Result<?> seckill(Long sessionId, Long userId, int quantity);

    /**
     * 获取进行中的秒杀场次
     * 时间范围：startTime <= now <= endTime
     *
     * @return 进行中的场次列表
     */
    List<SeckillSession> getActiveSessions();

    /**
     * 获取即将开始的秒杀场次
     * 时间范围：now < startTime
     *
     * @return 即将开始的场次列表
     */
    List<SeckillSession> getUpcomingSessions();

    /**
     * 获取所有秒杀场次（管理员用）
     */
    List<SeckillSession> getAllSessions();

    /**
     * 根据ID获取秒杀场次
     */
    SeckillSession getById(Long id);

    /**
     * 创建秒杀场次
     */
    void create(SeckillSession session);

    /**
     * 更新秒杀场次
     */
    void update(SeckillSession session);

    /**
     * 删除秒杀场次
     */
    void delete(Long id);
}
