package com.jsu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsu.common.result.PageResult;
import com.jsu.common.result.Result;
import com.jsu.entity.Show;
import com.jsu.mapper.OrderMapper;
import com.jsu.mapper.SeckillSessionMapper;
import com.jsu.mapper.ShowMapper;
import com.jsu.mapper.TicketMapper;
import com.jsu.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 演出管理Controller
 * 提供演出的增删改查、分类查询、搜索等功能
 *
 * @Tag Swagger文档分组标签：演出管理
 */
@RestController
@RequestMapping("/api/show")
@Tag(name = "演出管理")
@Slf4j
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;
    private final ShowMapper showMapper;
    private final TicketMapper ticketMapper;
    private final SeckillSessionMapper seckillSessionMapper;
    private final OrderMapper orderMapper;

    /**
     * 获取系统统计数据
     * 用于后台管理 dashboard，展示系统整体运行状态
     *
     * @return 包含演出数量、票档数量、秒杀场次数、订单数量的统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取统计数据")
    public Result<Map<String, Object>> getStats() {
        Long showCount = showMapper.selectCount(null);
        Long ticketCount = ticketMapper.selectCount(null);
        Long seckillCount = seckillSessionMapper.selectCount(null);
        Long orderCount = orderMapper.selectCount(null);
        Map<String, Object> stats = Map.of(
                "showCount", showCount,
                "ticketCount", ticketCount,
                "seckillCount", seckillCount,
                "orderCount", orderCount
        );
        return Result.success(stats);
    }

    /**
     * 分页查询演出列表
     * 支持按名称、分类、城市、状态筛选
     *
     * @param page 页码（默认1）
     * @param size 每页数量（默认10）
     * @param show 筛选条件（可选：name、category、city、status）
     * @return 分页后的演出列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询演出列表")
    public Result<PageResult<Show>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Show show) {
        log.info("分页查询演出列表: page={}, size={}", page, size);
        Page<Show> pageInfo = new Page<>(page, size);
        var result = showService.page(pageInfo, show);

        return Result.success(PageResult.of(
                result.getTotal(),
                size,
                page,
                result.getRecords()
        ));
    }

    /**
     * 获取演出详情
     *
     * @param id 演出ID
     * @return 演出详细信息
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "查询演出详情")
    public Result<Show> detail(@PathVariable Long id) {
        Show s = showService.getById(id);
        return Result.success(s);
    }

    /**
     * 按分类查询演出
     * 常见分类：演唱会、话剧、音乐剧、舞蹈等
     *
     * @param category 演出分类
     * @return 该分类下的所有演出列表
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "按分类查询演出")
    public Result<List<Show>> getByCategory(@PathVariable String category) {
        List<Show> shows = showService.getByCategory(category);
        return Result.success(shows);
    }

    /**
     * 获取热门演出
     * 按创建时间倒序返回指定数量的演出
     *
     * @param limit 返回数量（默认10）
     * @return 热门演出列表
     */
    @GetMapping("/hot")
    @Operation(summary = "查询热门演出")
    public Result<List<Show>> getHotShows(@RequestParam(defaultValue = "10") int limit) {
        List<Show> shows = showService.getHotShows(limit);
        return Result.success(shows);
    }

    /**
     * 搜索演出
     * 根据关键词模糊匹配演出名称
     *
     * @param keyword 搜索关键词
     * @return 匹配的演出列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索演出")
    public Result<List<Show>> search(@RequestParam String keyword) {
        List<Show> shows = showService.search(keyword);
        return Result.success(shows);
    }

    /**
     * 创建演出（管理员）
     *
     * @param show 演出信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建演出")
    public Result<Void> create(@RequestBody Show show) {
        showService.create(show);
        return Result.success();
    }

    /**
     * 更新演出（管理员）
     *
     * @param show 更新后的演出信息
     */
    @PutMapping("/update")
    @Operation(summary = "更新演出")
    public Result<Void> update(@RequestBody Show show) {
        showService.update(show);
        return Result.success();
    }

    /**
     * 删除演出（管理员）
     *
     * @param id 演出ID
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除演出")
    public Result<Void> delete(@PathVariable Long id) {
        showService.delete(id);
        return Result.success();
    }
}
