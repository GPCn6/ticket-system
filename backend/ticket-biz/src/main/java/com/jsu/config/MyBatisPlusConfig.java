package com.jsu.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * 核心功能：注册MyBatis-Plus插件
 * - 分页插件（PaginationInnerInterceptor）：支持Page对象实现物理分页
 * - 没有使用乐观锁插件，库存扣减通过自定义SQL的 WHERE available_stock >= #{quantity} 实现乐观锁
 *
 * @Configuration 标记为配置类
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册MyBatis-Plus拦截器
     *
     * PaginationInnerInterceptor 实现了通用的分页查询功能，
     * 自动拦截带有IPage参数的方法，在SQL末尾追加 LIMIT 语句
     *
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
