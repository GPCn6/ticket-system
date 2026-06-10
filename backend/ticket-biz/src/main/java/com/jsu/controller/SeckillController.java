package com.jsu.controller;

import com.jsu.common.result.Result;
import com.jsu.entity.SeckillSession;
import com.jsu.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 抢购/秒杀管理Controller
 */
@RestController
@RequestMapping("/api/seckill")
@Tag(name = "抢购管理")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/execute")
    @Operation(summary = "执行抢购")
    public Result<?> execute(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Long sessionId = Long.parseLong(request.get("sessionId").toString());
        int quantity = Integer.parseInt(request.get("quantity").toString());

        String userIdHeader = httpRequest.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            return Result.fail(401, "未登录或缺少用户身份");
        }
        Long userId;
        try {
            userId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            return Result.fail(401, "用户身份无效");
        }
        return seckillService.seckill(sessionId, userId, quantity);
    }

    /**
     * 预热单场库存
     */
    @PostMapping("/warmup/{sessionId}")
    @Operation(summary = "预热单场库存")
    public Result<Map<String, Object>> warmup(@PathVariable Long sessionId) {
        int stock = seckillService.warmUpStock(sessionId);
        return Result.success(Map.of("sessionId", sessionId, "stock", stock));
    }

    /**
     * 批量预热所有即将开始的场次
     */
    @PostMapping("/warmup/batch")
    @Operation(summary = "批量预热所有即将开始的场次")
    public Result<Map<String, Object>> batchWarmup() {
        int count = seckillService.batchWarmUp();
        return Result.success(Map.of("warmedCount", count));
    }

    @GetMapping("/active")
    @Operation(summary = "查询进行中的抢购场次")
    public Result<List<SeckillSession>> getActiveSessions() {
        return Result.success(seckillService.getActiveSessions());
    }

    @GetMapping("/upcoming")
    @Operation(summary = "查询即将开始的抢购场次")
    public Result<List<SeckillSession>> getUpcomingSessions() {
        return Result.success(seckillService.getUpcomingSessions());
    }

    @GetMapping("/all")
    @Operation(summary = "查询所有场次（管理员用）")
    public Result<List<SeckillSession>> getAllSessions() {
        return Result.success(seckillService.getAllSessions());
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "查询抢购场次详情")
    public Result<SeckillSession> detail(@PathVariable Long id) {
        return Result.success(seckillService.getById(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建抢购场次")
    public Result<Void> create(@RequestBody SeckillSession session) {
        seckillService.create(session);
        return Result.success();
    }

    @PutMapping("/update")
    @Operation(summary = "更新抢购场次")
    public Result<Void> update(@RequestBody SeckillSession session) {
        seckillService.update(session);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除抢购场次")
    public Result<Void> delete(@PathVariable Long id) {
        seckillService.delete(id);
        return Result.success();
    }
}
