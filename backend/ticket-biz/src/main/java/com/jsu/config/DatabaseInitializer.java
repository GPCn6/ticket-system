package com.jsu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库结构初始化器：启动时自动检查并补充缺失的列
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        addColumnIfNotExists("biz_order", "session_id", "BIGINT DEFAULT NULL COMMENT '秒杀场次ID'");
        addColumnIfNotExists("biz_order", "is_seckill", "INT DEFAULT 0 COMMENT '是否秒杀订单 0-否 1-是'");
        addColumnIfNotExists("biz_order", "pay_time", "datetime DEFAULT NULL COMMENT '支付时间'");

        // 补全历史订单缺失的 create_time（兼容已有的 NULL 数据）
        backfillCreateTime("biz_order");
        backfillCreateTime("biz_show");
        backfillCreateTime("biz_ticket");
        backfillCreateTime("biz_seckill_session");

        // 修复历史订单中 is_seckill 为 NULL 的数据（兼容代码添加前的历史数据）
        backfillIsSeckill();

        log.info("数据库结构检查完成");
    }

    /**
     * 修复历史订单中 is_seckill 为 NULL 的数据
     */
    private void backfillIsSeckill() {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE biz_order SET is_seckill = 0 WHERE is_seckill IS NULL");
            if (updated > 0) {
                log.info("已修复 biz_order.is_seckill 历史数据: {} 条", updated);
            }
        } catch (Exception e) {
            log.warn("修复 biz_order.is_seckill 失败: {}", e.getMessage());
        }
    }

    private void backfillCreateTime(String tableName) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE " + tableName + " SET create_time = NOW() WHERE create_time IS NULL");
            if (updated > 0) {
                log.info("已补全 {}.create_time 历史数据: {} 条", tableName, updated);
            }
        } catch (Exception e) {
            log.warn("补全 {}.create_time 失败: {}", tableName, e.getMessage());
        }
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
            if (count != null && count == 0) {
                String alterSql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition);
                jdbcTemplate.execute(alterSql);
                log.info("已添加列: {}.{}", tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("检查/添加列失败: {}.{} - {}", tableName, columnName, e.getMessage());
        }
    }
}
