package com.jsu.controller;

import com.jsu.common.result.Result;
import com.jsu.entity.Ticket;
import com.jsu.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 票档管理Controller
 * 提供演出票档的增删改查功能
 *
 * @Tag Swagger文档分组标签：票档管理
 */
@RestController
@RequestMapping("/api/ticket")
@Tag(name = "票档管理")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 获取票档详情
     *
     * @param id 票档ID
     * @return 票档详细信息
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "查询票档详情")
    public Result<Ticket> detail(@PathVariable Long id) {
        Ticket ticket = ticketService.getById(id);
        return Result.success(ticket);
    }

    /**
     * 获取指定演出的所有票档
     *
     * @param showId 演出ID
     * @return 票档列表
     */
    @GetMapping("/show/{showId}")
    @Operation(summary = "查询演出的票档")
    public Result<List<Ticket>> getByShowId(@PathVariable Long showId) {
        List<Ticket> tickets = ticketService.getByShowId(showId);
        return Result.success(tickets);
    }

    /**
     * 创建票档（管理员）
     *
     * @param ticket 票档信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建票档")
    public Result<Void> create(@RequestBody Ticket ticket) {
        ticketService.create(ticket);
        return Result.success();
    }

    /**
     * 更新票档（管理员）
     *
     * @param ticket 更新后的票档信息
     */
    @PutMapping("/update")
    @Operation(summary = "更新票档")
    public Result<Void> update(@RequestBody Ticket ticket) {
        ticketService.update(ticket);
        return Result.success();
    }

    /**
     * 删除票档（管理员）
     *
     * @param id 票档ID
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除票档")
    public Result<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return Result.success();
    }
}
