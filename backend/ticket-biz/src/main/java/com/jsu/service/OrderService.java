package com.jsu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsu.entity.Order;

/**
 * 订单服务接口
 * 定义订单相关的业务操作
 */
public interface OrderService {

    /**
     * 创建订单
     * 核心流程：校验演出/票档 → 检查库存 → 扣减库存 → 生成订单
     *
     * @param order 订单信息
     * @return 创建成功的订单
     */
    Order create(Order order);

    /**
     * 根据ID获取订单
     */
    Order getById(Long id);

    /**
     * 根据订单号获取订单
     */
    Order getByOrderNo(String orderNo);

    /**
     * 获取用户的订单列表（分页）
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param status 订单状态筛选（可选）
     * @return 分页后的订单列表
     */
    IPage<Order> getUserOrders(Page<Order> page, Long userId, Integer status);

    /**
     * 获取所有订单（管理员用，分页）
     */
    IPage<Order> getAllOrders(Page<Order> page, Integer status);

    /**
     * 取消订单
     * 仅可取消"待支付"状态的订单
     *
     * @param id 订单ID
     * @return true=取消成功，false=订单不存在或状态不允许取消
     */
    boolean cancel(Long id);

    /**
     * 支付订单
     *
     * @param orderNo 订单号
     * @return true=支付成功
     */
    boolean pay(String orderNo);
}
