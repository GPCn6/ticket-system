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
 * 提供秒杀场次的创建、查询、执行等功能
 *
 * @Tag Swagger文档分组标签：抢购管理
 */
@RestController
@RequestMapping("/api/seckill")
@Tag(name = "抢购管理")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 执行抢购
     * 核心秒杀接口，使用Redis+Lua脚本实现：
     * 1. 验证场次是否在有效期内
     * 2. 检查用户是否已购买（每人限购）
     * 3. 扣减Redis库存
     * 4. 发送订单创建消息到MQ
     *
     * @param request 包含sessionId和quantity
     * @param httpRequest HTTP请求
     * @return 抢购结果（成功/库存不足/已限购等）
     */
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
     * 预热秒杀场次库存
     * 将数据库中的库存数据加载到Redis，用于高并发秒杀
     * 建议在秒杀开始前调用
     *
     * @param sessionId 秒杀场次ID
     */
    @PostMapping("/warmup/{sessionId}")
    @Operation(summary = "预热抢购场次库存")
    public Result<Void> warmup(@PathVariable Long sessionId) {
        seckillService.warmUpStock(sessionId);
        return Result.success();
    }

    /**
     * 获取当前进行中的秒杀场次
     *
     * @return 进行中的秒杀场次列表
     */
    @GetMapping("/active")
    @Operation(summary = "查询进行中的抢购场次")
    public Result<List<SeckillSession>> getActiveSessions() {
        List<SeckillSession> sessions = seckillService.getActiveSessions();
        return Result.success(sessions);
    }

    /**
     * 获取即将开始的秒杀场次
     *
     * @return 即将开始的秒杀场次列表
     */
    @GetMapping("/upcoming")
    @Operation(summary = "查询即将开始的抢购场次")
    public Result<List<SeckillSession>> getUpcomingSessions() {
        List<SeckillSession> sessions = seckillService.getUpcomingSessions();
        return Result.success(sessions);
    }

    /**
     * 获取所有秒杀场次（管理员用）
     *
     * @return 所有秒杀场次列表
     */
    @GetMapping("/all")
    @Operation(summary = "查询所有场次（管理员用）")
    public Result<List<SeckillSession>> getAllSessions() {
        List<SeckillSession> sessions = seckillService.getAllSessions();
        return Result.success(sessions);
    }

    /**
     * 获取秒杀场次详情
     *
     * @param id 场次ID
     * @return 场次详细信息
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "查询抢购场次详情")
    public Result<SeckillSession> detail(@PathVariable Long id) {
        SeckillSession session = seckillService.getById(id);
        return Result.success(session);
    }

    /**
     * 创建秒杀场次（管理员）
     *
     * @param session 场次信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建抢购场次")
    public Result<Void> create(@RequestBody SeckillSession session) {
        seckillService.create(session);
        return Result.success();
    }

    /**
     * 更新秒杀场次（管理员）
     *
     * @param session 更新后的场次信息
     */
    @PutMapping("/update")
    @Operation(summary = "更新抢购场次")
    public Result<Void> update(@RequestBody SeckillSession session) {
        seckillService.update(session);
        return Result.success();
    }

    /**
     * 删除秒杀场次（管理员）
     *
     * @param id 场次ID
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除抢购场次")
    public Result<Void> delete(@PathVariable Long id) {
        seckillService.delete(id);
        return Result.success();
    }
}
