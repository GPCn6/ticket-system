package com.jsu.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsu.entity.Show;
import com.jsu.mapper.ShowMapper;
import com.jsu.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 演出服务实现类
 * 处理演出的分页查询、条件筛选、分类查询、搜索等功能
 */
@Service
@RequiredArgsConstructor
public class ShowServiceImpl implements ShowService {

    private final ShowMapper showMapper;

    /**
     * 分页查询演出列表
     * 支持多条件组合筛选：名称模糊匹配、分类精确匹配、城市精确匹配、状态精确匹配
     *
     * @param page 分页参数
     * @param show 筛选条件对象
     * @return 分页结果
     */
    @Override
    public IPage<Show> page(Page<Show> page, Show show) {
        LambdaQueryWrapper<Show> wrapper = new LambdaQueryWrapper<>();
        // 按名称模糊查询
        if (show.getName() != null && !show.getName().isEmpty()) {
            wrapper.like(Show::getName, show.getName());
        }
        // 按分类精确查询
        if (show.getCategory() != null && !show.getCategory().isEmpty()) {
            wrapper.eq(Show::getCategory, show.getCategory());
        }
        // 按城市精确查询
        if (show.getCity() != null && !show.getCity().isEmpty()) {
            wrapper.eq(Show::getCity, show.getCity());
        }
        // 按状态精确查询
        if (show.getStatus() != null) {
            wrapper.eq(Show::getStatus, show.getStatus());
        }
        // 按创建时间倒序排列
        wrapper.orderByDesc(Show::getCreateTime);
        return showMapper.selectPage(page, wrapper);
    }

    /**
     * 根据ID获取演出信息
     */
    @Override
    public Show getById(Long id) {
        return showMapper.selectById(id);
    }

    /**
     * 按分类查询演出
     *
     * @param category 演出分类
     * @return 该分类下的所有演出
     */
    @Override
    public List<Show> getByCategory(String category) {
        return showMapper.selectByCategory(category);
    }

    /**
     * 获取热门演出
     * 按创建时间倒序返回指定数量的演出
     *
     * @param limit 返回数量
     * @return 热门演出列表
     */
    @Override
    public List<Show> getHotShows(int limit) {
        return showMapper.selectHotShows(limit);
    }

    /**
     * 搜索演出
     * 根据关键词模糊匹配演出名称
     *
     * @param keyword 搜索关键词
     * @return 匹配的演出列表
     */
    @Override
    public List<Show> search(String keyword) {
        return showMapper.search(keyword);
    }

    /**
     * 创建演出
     */
    @Override
    @Transactional
    public void create(Show show) {
        showMapper.insert(show);
    }

    /**
     * 更新演出
     */
    @Override
    @Transactional
    public void update(Show show) {
        showMapper.updateById(show);
    }

    /**
     * 删除演出
     */
    @Override
    @Transactional
    public void delete(Long id) {
        showMapper.deleteById(id);
    }
}
