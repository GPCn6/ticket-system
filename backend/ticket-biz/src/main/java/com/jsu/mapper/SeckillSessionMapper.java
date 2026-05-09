package com.jsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsu.entity.SeckillSession;

import java.util.List;

/**
 * 秒杀场次 Mapper 接口
 *
 * 秒杀场次的库存管理策略：
 * - Redis：秒杀期间库存放在 Redis 中，通过 Lua 脚本原子扣减（高并发）
 * - 数据库：通过 MQ 异步同步 Redis 库存到数据库（最终一致性）
 * - deductStock/restoreStock 使用乐观锁防止并发更新冲突
 */
public interface SeckillSessionMapper extends BaseMapper<SeckillSession> {

    /**
     * 查询进行中的秒杀场次
     * 条件：status=1 AND start_time <= NOW() AND end_time >= NOW()
     *
     * @return 进行中的场次列表
     */
    List<SeckillSession> selectActiveSessions();

    /**
     * 查询所有秒杀场次（管理员用）
     *
     * @return 全部场次列表
     */
    List<SeckillSession> selectAll();

    /**
     * 查询即将开始的秒杀场次
     * 条件：status=1 AND start_time > NOW()
     *
     * @return 即将开始的场次列表
     */
    List<SeckillSession> selectUpcomingSessions();

    /**
     * 扣减秒杀场次数据库库存
     * 乐观锁：WHERE stock >= #{quantity}
     *
     * @param sessionId 场次ID
     * @param quantity 扣减数量
     * @return 影响行数
     */
    int deductStock(Long sessionId, int quantity);

    /**
     * 恢复秒杀场次数据库库存
     *
     * @param sessionId 场次ID
     * @param quantity 恢复数量
     * @return 影响行数
     */
    int restoreStock(Long sessionId, int quantity);
}
