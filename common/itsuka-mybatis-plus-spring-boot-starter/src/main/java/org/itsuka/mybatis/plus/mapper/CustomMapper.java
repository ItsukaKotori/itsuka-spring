package org.itsuka.mybatis.plus.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.List;

/**
 * 自定义Mapper
 *
 * @author Itsuka
 * @date 2023/3/5
 */
public interface CustomMapper<T> extends BaseMapper<T> {
    /**
     * 单条件EQ 列表查询
     *
     * @param field 字段
     * @param value 值
     * @return 列表
     */
    default List<T> selectList(SFunction<T, ?> field, Object value) {
        return selectList(new LambdaQueryWrapper<T>().eq(field, value));
    }

    /**
     * 双条件EQ 列表查询
     *
     * @param field1 字段1
     * @param value1 值1
     * @param field2 字段2
     * @param value2 值2
     * @return 列表
     */
    default List<T> selectList(SFunction<T, ?> field1, Object value1,
                               SFunction<T, ?> field2, Object value2) {
        return selectList(new LambdaQueryWrapper<T>()
                .eq(field1, value1).eq(field2, value2));
    }

    /**
     * 单条件EQ 查询
     *
     * @param field 字段
     * @param value 值
     * @return 列表
     */
    default T selectOne(SFunction<T, ?> field, Object value) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field, value));
    }

    /**
     * 双条件EQ 查询
     *
     * @param field1 字段1
     * @param value1 值1
     * @param field2 字段2
     * @param value2 值2
     * @return 列表
     */
    default T selectOne(SFunction<T, ?> field1, Object value1,
                               SFunction<T, ?> field2, Object value2) {
        return selectOne(new LambdaQueryWrapper<T>()
                .eq(field1, value1).eq(field2, value2));
    }

    /**
     * 单条件EQ 查询个数
     *
     * @param field 字段
     * @param value 值
     * @return 列表
     */
    default Long selectCount(SFunction<T, ?> field, Object value) {
        return selectCount(new LambdaQueryWrapper<T>().eq(field, value));
    }

    /**
     * 双条件EQ 查询个数
     *
     * @param field1 字段1
     * @param value1 值1
     * @param field2 字段2
     * @param value2 值2
     * @return 列表
     */
    default Long selectCount(SFunction<T, ?> field1, Object value1,
                        SFunction<T, ?> field2, Object value2) {
        return selectCount(new LambdaQueryWrapper<T>()
                .eq(field1, value1).eq(field2, value2));
    }

    /**
     * 单条件删除
     *
     * @param field 字段
     * @param value 值
     * @return 删除个数
     */
    default Integer delete(SFunction<T, ?> field, Object value) {
        return delete(new LambdaQueryWrapper<T>().eq(field, value));
    }
}
