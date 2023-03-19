package org.itsuka.mybatis.plus.interceptor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.itsuka.core.domian.LoginUser;
import org.itsuka.core.exception.ServiceException;
import org.itsuka.mybatis.plus.mapper.CustomMapper;
import org.itsuka.mybatis.plus.properties.DataPermissionProperty;
import org.itsuka.web.utils.WebHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 数据权限拦截器
 *
 * @author Itsuka
 * @date 2023/3/5
 */
@RequiredArgsConstructor
@Slf4j
public class DataPermissionInterceptor implements InnerInterceptor {
    private final DataPermissionProperty property;
    private final WebHelper webHelper;

    @Override
    public boolean willDoUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        // 只拦截更新与删除
        if (SqlCommandType.INSERT == ms.getSqlCommandType()) {
            return true;
        }
        // 如果用户未登录 不允许进行操作
        Optional<LoginUser> optional = webHelper.getLoginUser();
        if (optional.isEmpty()) {
            return false;
        }
        LoginUser loginUser = optional.get();
        // 判断是否为用户信息更新
        if (isUserProfileUpdate(ms, loginUser.getId(), parameter)) {
            return true;
        }
        // 判断是否包含createBy字段
        // 不包含为中间表 无法判断 直接跳过
        if (!containsCreateBy(ms)) {
            return true;
        }
        // 判断数据是否由当前用户创建
        if (!isCreateByCurrUser(loginUser.getId(), executor)) {
            return false;
        }
        return InnerInterceptor.super.willDoUpdate(executor, ms, parameter);
    }

    private boolean isCreateByCurrUser(Long userId, Executor executor) throws SQLException {
        // TODO
        Connection connection = executor.getTransaction().getConnection();
        return false;
    }

    private boolean containsCreateBy(MappedStatement ms) {
        TableInfo tableInfo = getTableInfo(ms);
        return tableInfo.getFieldList().stream().anyMatch(tableFieldInfo ->
                Objects.equals(property.getCreateByField(), tableFieldInfo.getColumn()));
    }

    private boolean isUserProfileUpdate(MappedStatement ms, Long userId, Object parameter) {
        TableInfo tableInfo = getTableInfo(ms);
        String currTableName = tableInfo.getTableName();
        String userTableName = property.getUserTableName();
        if (!Objects.equals(userTableName, currTableName)) {
            // 非用户表更新
            return false;
        }
        String sql = String.format("select id from %s %s", userTableName, where());
        List<Map<String, Object>> maps = doQuery(sql, parameter);
        return maps.stream().anyMatch(map -> Objects.equals(userId, map.get("id")));
    }

    /**
     * TODO
     *
     * @return
     */
    private String where() {
        return "";
    }

    private List<Map<String, Object>> doQuery(String sql, Object... parameters) {
        try (SqlRunner sqlRunner = SqlRunner.db()) {
            return sqlRunner.selectList(sql, parameters);
        }
    }

    private TableInfo getTableInfo(MappedStatement ms) {
        Class<?> mapperClass = getMapperClass(ms);
        return TableInfoHelper.getTableInfo(getEntityClass(mapperClass));
    }

    private Class<?> getEntityClass(Class<?> mapperClass) {
        Class<?> entityClass = ReflectionKit.getSuperClassGenericType(mapperClass, CustomMapper.class, 0);
        if (entityClass == null) {
            entityClass = ReflectionKit.getSuperClassGenericType(mapperClass, BaseMapper.class, 0);
        }
        return entityClass;
    }

    private Class<?> getMapperClass(MappedStatement ms) {
        // id Mapper全类名
        String mapperClassName = ms.getId();
        try {
            return Class.forName(mapperClassName);
        } catch (ClassNotFoundException e) {
            log.error("mapper class [{}] not found", mapperClassName);
            throw ServiceException.create("mapper class [" + mapperClassName + "] not found");
        }
    }
}
