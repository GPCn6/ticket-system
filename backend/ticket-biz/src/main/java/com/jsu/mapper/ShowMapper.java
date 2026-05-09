package com.jsu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsu.entity.Show;

import java.util.List;

/**
 * 演出 Mapper 接口
 *
 * 继承 BaseMapper<Show> 获得基础的 CRUD 方法（insert/deleteById/updateById/selectById等）
 * 自定义方法在 ShowMapper.xml 中实现
 */
public interface ShowMapper extends BaseMapper<Show> {

    /**
     * 条件分页查询演出
     *
     * @param page 分页参数
     * @param show 筛选条件（name模糊匹配、category/city/status精确匹配）
     * @return 分页结果
     */
    IPage<Show> selectByCondition(Page<Show> page, Show show);

    /**
     * 按分类查询演出
     * 只查询状态为"上架"的演出
     *
     * @param category 分类名称（如：演唱会、话剧）
     * @return 该分类下的演出列表
     */
    List<Show> selectByCategory(String category);

    /**
     * 获取热门演出
     * 按创建时间倒序排列，取前N条
     *
     * @param limit 返回条数
     * @return 热门演出列表
     */
    List<Show> selectHotShows(int limit);

    /**
     * 全文搜索演出
     * 匹配 name、venue、description 字段
     *
     * @param keyword 搜索关键词
     * @return 匹配的演出列表
     */
    List<Show> search(String keyword);
}
