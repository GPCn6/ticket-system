package com.jsu.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 *
 * 功能：为数据库表的 create_time 和 update_time 字段自动赋值
 * - 插入时：自动填充 createTime 和 updateTime
 * - 更新时：自动填充 updateTime
 *
 * 对应实体类的 @TableField(fill = FieldFill.INSERT) 和 @TableField(fill = FieldFill.INSERT_UPDATE) 注解
 *
 * @Component 注册为Spring组件
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     * - createTime：当前时间
     * - updateTime：当前时间
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充
     * - updateTime：当前时间
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
