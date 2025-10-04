package org.itsuka.mybatis.plus.toolkit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.LambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.itsuka.core.exception.ServiceException;
import org.itsuka.mybatis.plus.mapper.CustomMapper;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MybatisPlus工具类
 * 
 * @author itsuka
 * @since 2023/06/10
 */
@Slf4j
public class MybatisPlusHelper {
    /**
     * 截取Update/Delete语句的Where条件
     * 
     * @param ms
     * @param parameter
     * @return
     */
    public static String where(MappedStatement ms, Object parameter) {
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();    
        return sql.substring(sql.indexOf("where"));
    }

    /**
     * 查询
     * 
     * @param sql
     * @param parameters
     * @return
     */
    public static List<Map<String, Object>> doQuery(String sql, Object... parameters) {
        SqlRunner sqlRunner = SqlRunner.db();
        return sqlRunner.selectList(sql, parameters);
    }

    /**
     * 获取表信息
     * 
     * @param ms MappedStatement
     * @return 表信息
     */
    public static TableInfo getTableInfo(MappedStatement ms) {
        Class<?> mapperClass = getMapperClass(ms);
        return TableInfoHelper.getTableInfo(getEntityClass(mapperClass));
    }

    /**
     * 获取实体类Class
     * 
     * @param mapperClass Mapper Class
     * @return 实体类Class
     */
    public static Class<?> getEntityClass(Class<?> mapperClass) {
        Class<?> entityClass = ReflectionKit.getSuperClassGenericType(mapperClass, CustomMapper.class, 0);
        if (entityClass == null) {
            entityClass = ReflectionKit.getSuperClassGenericType(mapperClass, BaseMapper.class, 0);
        }
        return entityClass;
    }

    /**
     * 获取Mapper Class
     * 
     * @param ms MappedStatement
     * @return Mapper Class
     */
    public static Class<?> getMapperClass(MappedStatement ms) {
        // id Mapper全类名
        String mapperClassName = ms.getId();
        try {
            return Class.forName(mapperClassName);
        } catch (ClassNotFoundException e) {
            log.error("mapper class [{}] not found", mapperClassName);
            throw ServiceException.create("mapper class [" + mapperClassName + "] not found");
        }
    }

    public static <T> String getColumnName(SFunction<T, ?> field) {
        LambdaMeta meta = LambdaUtils.extract(field);
        String fieldName = PropertyNamer.methodToProperty(meta.getImplMethodName());
        Map<String, ColumnCache> columnMap = LambdaUtils.getColumnMap(meta.getInstantiatedClass());
        ColumnCache columnCache = columnMap.get(fieldName.toUpperCase(Locale.ROOT));
        if (ObjectUtils.isEmpty(columnCache)) {
            throw new AssertionError();
        }
        return columnCache.getColumn();
    }
}
