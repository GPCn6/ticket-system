package com.jsu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsu.entity.Show;

import java.util.List;

/**
 * 演出服务接口
 * 定义演出相关的业务操作
 */
public interface ShowService {

    /**
     * 分页查询演出列表
     *
     * @param page 分页参数
     * @param show 筛选条件（可选：name、category、city、status）
     * @return 分页结果
     */
    IPage<Show> page(Page<Show> page, Show show);

    /**
     * 根据ID获取演出
     */
    Show getById(Long id);

    /**
     * 按分类查询演出
     *
     * @param category 演出分类
     * @return 该分类下的所有演出
     */
    List<Show> getByCategory(String category);

    /**
     * 获取热门演出
     *
     * @param limit 返回数量
     * @return 热门演出列表
     */
    List<Show> getHotShows(int limit);

    /**
     * 搜索演出
     *
     * @param keyword 搜索关键词
     * @return 匹配的演出列表
     */
    List<Show> search(String keyword);

    /**
     * 创建演出
     */
    void create(Show show);

    /**
     * 更新演出
     */
    void update(Show show);

    /**
     * 删除演出
     */
    void delete(Long id);
}
