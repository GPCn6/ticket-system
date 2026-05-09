package com.jsu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsu.common.exception.BusinessException;
import com.jsu.common.result.PageResult;
import com.jsu.common.result.Result;
import com.jsu.entity.Order;
import com.jsu.mq.producer.MessageProducer;
import com.jsu.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单管理Controller
 * 提供订单创建、查询、取消、支付等功能
 *
 * @Tag Swagger文档分组标签：订单管理
 */
@RestController
@RequestMapping("/api/order")
@Tag(name = "订单管理")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MessageProducer messageProducer;

    /**
     * 创建订单
     * 1. 验证演出和票档信息
     * 2. 检查库存是否充足
     * 3. 扣减票档库存
     * 4. 生成订单号，设置15分钟支付过期时间
     *
     * @param order 订单信息（showId、ticketId、quantity必填）
     * @param request HTTP请求，包含用户身份信息
     * @return 创建成功的订单（含订单号、应付金额等）
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单")
    public Result<Order> create(@RequestBody Order order, HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return Result.fail(401, "未登录或缺少用户身份");
        }
        Long userId = Long.parseLong(userIdHeader);
        order.setUserId(userId);
        order = orderService.create(order);
        return Result.success(order);
    }

    /**
     * 获取订单详情
     * 通过订单ID查询，用户只能查看自己的订单
     *
     * @param id 订单ID
     * @param request HTTP请求
     * @return 订单详细信息
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "查询订单详情")
    public Result<Order> detail(@PathVariable Long id, HttpServletRequest request) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!hasOrderAccess(request, order)) {
            return Result.fail(403, "无权限访问该订单");
        }
        return Result.success(order);
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @param request HTTP请求
     * @return 订单信息
     */
    @GetMapping("/detail/orderNo/{orderNo}")
    @Operation(summary = "根据订单号查询订单")
    public Result<Order> getByOrderNo(@PathVariable String orderNo, HttpServletRequest request) {
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!hasOrderAccess(request, order)) {
            return Result.fail(403, "无权限访问该订单");
        }
        return Result.success(order);
    }

    /**
     * 获取当前用户的订单列表（分页）
     *
     * @param page 页码
     * @param size 每页数量
     * @param status 订单状态筛选（可选）
     * @param request HTTP请求
     * @return 用户订单分页列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询用户订单列表")
    public Result<PageResult<Order>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return Result.fail(401, "未登录或缺少用户身份");
        }
        Long userId = Long.parseLong(userIdHeader);

        Page<Order> pageInfo = new Page<>(page, size);
        var result = orderService.getUserOrders(pageInfo, userId, status);
        return Result.success(PageResult.of(
                result.getTotal(),
                size,
                page,
                result.getRecords()));
    }

    /**
     * 管理员查询所有订单（分页）
     * 需admin角色权限
     *
     * @param page 页码
     * @param size 每页数量
     * @param status 订单状态筛选
     * @param request HTTP请求
     * @return 所有订单分页列表
     */
    @GetMapping("/admin/list")
    @Operation(summary = "管理员查询所有订单")
    public Result<PageResult<Order>> adminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        if (!"admin".equalsIgnoreCase(role)) {
            return Result.fail(403, "无权限");
        }
        Page<Order> pageInfo = new Page<>(page, size);
        var result = orderService.getAllOrders(pageInfo, status);
        return Result.success(PageResult.of(
                result.getTotal(),
                size,
                page,
                result.getRecords()));
    }

    /**
     * 取消订单
     * 仅可取消状态为"待支付"的订单
     * 取消后通过MQ恢复票档库存
     *
     * @param id 订单ID
     * @param request HTTP请求
     * @return 取消结果
     */
    @PostMapping("/cancel/{id}")
    @Operation(summary = "取消订单")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!hasOrderAccess(request, order)) {
            return Result.fail(403, "无权限取消该订单");
        }
        boolean cancelled = orderService.cancel(id);
        if (!cancelled) {
            return Result.fail("取消失败，订单状态不允许取消");
        }
        // 通过MQ恢复库存（包含票档库存、秒杀场次库存、Redis库存）
        messageProducer.sendStockRestoreMessage(
                order.getSessionId(),
                order.getTicketId(),
                order.getQuantity(),
                order.getOrderNo(),
                order.getUserId(),
                order.getIsSeckill() != null && order.getIsSeckill() == 1
        );
        return Result.success();
    }

    /**
     * 支付订单
     * 仅可支付状态为"待支付"且未过期的订单
     *
     * @param request 包含orderNo的请求体
     * @param httpRequest HTTP请求
     * @return 支付结果
     */
    @PostMapping("/pay")
    @Operation(summary = "支付订单")
    public Result<Void> pay(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String orderNo = request.get("orderNo");
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!hasOrderAccess(httpRequest, order)) {
            return Result.fail(403, "无权限支付该订单");
        }
        try {
            boolean success = orderService.pay(orderNo);
            return success ? Result.success() : Result.fail("支付失败");
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("支付异常: orderNo={}", orderNo, e);
            return Result.fail("支付异常，请稍后重试");
        }
    }

    /**
     * 校验用户是否有权访问该订单
     * 管理员可访问所有订单，普通用户只能访问自己的订单
     *
     * @param request HTTP请求
     * @param order 目标订单
     * @return true=有权限，false=无权限
     */
    private boolean hasOrderAccess(HttpServletRequest request, Order order) {
        String role = request.getHeader("X-User-Role");
        if ("admin".equalsIgnoreCase(role)) {
            return true;
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return false;
        }
        return order.getUserId() != null && order.getUserId().equals(Long.parseLong(userIdHeader));
    }
}
