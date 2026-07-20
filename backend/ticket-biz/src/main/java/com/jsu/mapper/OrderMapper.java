package com.jsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsu.entity.Order;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 订单 Mapper 接口
 *
 * 自定义查询方法在 OrderMapper.xml 中实现
 * 所有查询都 LEFT JOIN 关联表（biz_show, biz_ticket, sys_user）
 * 以便一次性获取 showName、ticketName、userName 等展示字段
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据订单号查询订单
     * 关联查询演出名称、票档名称、用户名称
     *
     * @param orderNo 订单号
     * @return 订单信息（含扩展字段）
     */
    Order selectByOrderNo(String orderNo);

    /**
     * 分页查询用户订单
     * 支持按订单状态筛选
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param status 订单状态（可选，传null查询全部）
     * @return 分页后的订单列表
     */
    IPage<Order> selectUserOrders(Page<Order> page, Long userId, Integer status);

    /**
     * 分页查询所有订单（管理员用）
     *
     * @param page 分页参数
     * @param status 订单状态筛选（可选）
     * @return 分页后的订单列表
     */
    IPage<Order> selectAllOrders(Page<Order> page, @Param("status") Integer status);

    /**
     * 查询已过期的待支付订单
     * 用于定时任务自动取消超时订单
     *
     * @return 过期订单列表
     */
    List<Order> selectExpiredOrders();

    int cancelPending(@Param("id") Long id, @Param("cancelled") Integer cancelled, @Param("pending") Integer pending);

    int payPending(@Param("orderNo") String orderNo, @Param("paid") Integer paid,
                   @Param("pending") Integer pending, @Param("payTime") LocalDateTime payTime);
}
