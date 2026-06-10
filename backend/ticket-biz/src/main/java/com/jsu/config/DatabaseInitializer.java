package com.jsu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Database initializer: auto-check and add missing columns on startup.
 * Also handles password migration from plaintext to BCrypt.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        addColumnIfNotExists("biz_order", "session_id", "BIGINT DEFAULT NULL COMMENT 'seckill session ID'");
        addColumnIfNotExists("biz_order", "is_seckill", "INT DEFAULT 0 COMMENT 'is seckill order: 0=no, 1=yes'");
        addColumnIfNotExists("biz_order", "pay_time", "datetime DEFAULT NULL COMMENT 'payment time'");

        backfillCreateTime("biz_order");
        backfillCreateTime("biz_show");
        backfillCreateTime("biz_ticket");
        backfillCreateTime("biz_seckill_session");

        backfillIsSeckill();
        migratePlaintextPasswords();

        log.info("Database structure check completed");
    }

    private void backfillIsSeckill() {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE biz_order SET is_seckill = 0 WHERE is_seckill IS NULL");
            if (updated > 0) {
                log.info("Backfilled biz_order.is_seckill: {} rows", updated);
            }
        } catch (Exception e) {
            log.warn("Failed to backfill biz_order.is_seckill: {}", e.getMessage());
        }
    }

    private void backfillCreateTime(String tableName) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE " + tableName + " SET create_time = NOW() WHERE create_time IS NULL");
            if (updated > 0) {
                log.info("Backfilled {}.create_time: {} rows", tableName, updated);
            }
        } catch (Exception e) {
            log.warn("Failed to backfill {}.create_time: {}", tableName, e.getMessage());
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
                log.info("Added column: {}.{}", tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("Failed to check/add column {}.{}: {}", tableName, columnName, e.getMessage());
        }
    }

    /**
     * Auto-migrate plaintext passwords to BCrypt on startup (backward compatibility).
     * Any user whose password does NOT start with "$2a$" (BCrypt prefix) will be re-encrypted.
     */
    private void migratePlaintextPasswords() {
        try {
            var users = jdbcTemplate.queryForList(
                    "SELECT id, password FROM sys_user WHERE password NOT LIKE '$2a$%'");
            if (users.isEmpty()) {
                return;
            }
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            for (var user : users) {
                Long id = ((Number) user.get("id")).longValue();
                String plainPwd = (String) user.get("password");
                String encoded = encoder.encode(plainPwd);
                jdbcTemplate.update("UPDATE sys_user SET password = ? WHERE id = ?", encoded, id);
                log.info("Migrated password for user: id={}", id);
            }
            log.info("Plaintext password migration completed: {} users", users.size());
        } catch (Exception e) {
            log.warn("Password migration check failed: {}", e.getMessage());
        }
    }
}