package com.jsu.controller;

import com.jsu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "数据重置")
@Slf4j
@RequiredArgsConstructor
public class DataResetController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @PostMapping("/reset")
    @Operation(summary = "重置所有订单数据（清空订单、恢复库存、清理Redis）")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> reset(HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        if (!"admin".equalsIgnoreCase(role)) {
            return Result.fail(403, "仅管理员可执行此操作");
        }

        try {
            // 1. 保存各秒杀场次已售数量（用于恢复库存）
            List<Map<String, Object>> sessionSales = jdbcTemplate.queryForList(
                    "SELECT session_id, COALESCE(SUM(quantity), 0) AS total_qty " +
                    "FROM biz_order WHERE session_id IS NOT NULL GROUP BY session_id"
            );

            // 2. 删除所有订单
            int deletedOrders = jdbcTemplate.update("DELETE FROM biz_order");
            log.info("已删除 {} 条订单记录", deletedOrders);

            // 3. 恢复票档库存：available_stock = total_stock
            int updatedTickets = jdbcTemplate.update(
                    "UPDATE biz_ticket SET available_stock = total_stock"
            );
            log.info("已重置 {} 个票档库存", updatedTickets);

            // 4. 恢复秒杀场次库存：添加回已售数量
            int restoredSessions = 0;
            for (Map<String, Object> row : sessionSales) {
                Object sessionIdObj = row.get("session_id");
                Object qtyObj = row.get("total_qty");
                if (sessionIdObj != null && qtyObj != null) {
                    long sessionId = ((Number) sessionIdObj).longValue();
                    int totalQty = ((Number) qtyObj).intValue();
                    if (totalQty > 0) {
                        int updated = jdbcTemplate.update(
                                "UPDATE biz_seckill_session SET stock = stock + ? WHERE id = ?",
                                totalQty, sessionId
                        );
                        restoredSessions += updated;
                        log.info("恢复秒杀场次库存: sessionId={}, 恢复数量={}", sessionId, totalQty);
                    }
                }
            }
            log.info("已恢复 {} 个秒杀场次库存", restoredSessions);

            // 5. 清理Redis中所有秒杀相关的key
            Set<String> seckillKeys = stringRedisTemplate.keys("seckill:*");
            if (seckillKeys != null && !seckillKeys.isEmpty()) {
                stringRedisTemplate.delete(seckillKeys);
                log.info("已清理 {} 个Redis秒杀Key", seckillKeys.size());
            }

            return Result.success("重置成功，已删除 " + deletedOrders + " 条订单，" +
                    "重置 " + updatedTickets + " 个票档库存，" +
                    "恢复 " + restoredSessions + " 个秒杀场次，" +
                    "清理 " + (seckillKeys != null ? seckillKeys.size() : 0) + " 个Redis Key");
        } catch (Exception e) {
            log.error("数据重置失败", e);
            return Result.fail("重置失败: " + e.getMessage());
        }
    }
}
