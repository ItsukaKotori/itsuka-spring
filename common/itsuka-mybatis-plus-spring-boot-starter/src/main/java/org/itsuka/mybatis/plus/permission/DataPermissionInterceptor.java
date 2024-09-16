package org.itsuka.mybatis.plus.permission;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.itsuka.core.domian.LoginUser;
import org.itsuka.core.exception.ServiceException;
import org.itsuka.mybatis.plus.helper.MybatisPlusHelper;
import org.itsuka.mybatis.plus.mapper.CustomMapper;
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
 * @since 2023/3/5
 */
@RequiredArgsConstructor
@Slf4j
public class DataPermissionInterceptor implements InnerInterceptor {
    private final DataPermissionProperty property;
    
    private final WebHelper webHelper;

    private final DataPermissionEscaper escaper;

    private final MybatisPlusHelper mybatisPlusHelper;

    @Override
    public boolean willDoUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        // 只拦截更新与删除
        if (SqlCommandType.INSERT == ms.getSqlCommandType()) {
            return true;
        }
        // 判断本次执行SQL是否逃离权限控制
        if (escaper.isEscape()) {
            log.warn("escape permission");
            return true;
        }
        // 判断是否包含create_by字段
        // 不包含为中间表 无法判断 直接跳过
        if (!containsCreateBy(ms)) {
            log.warn("current table does not contain create_by column, do not intercept it");
            return true;
        }
        // 如果用户未登录 不允许进行操作
        Optional<LoginUser> optional = webHelper.getLoginUser();
        if (optional.isPresent()) {
            LoginUser loginUser = optional.get();
            // 判断数据是否由当前用户创建
            return isCreateByCurrentUser(loginUser.getId(), executor, ms, parameter);
        }
        return false;
    }

    /**
     * 当前数据是否被当前用户创建
     * 
     * @param userId
     * @param executor
     * @param ms
     * @param parameter
     * @return
     * @throws SQLException
     */
    private boolean isCreateByCurrentUser(Long userId, Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        String selectCreateBySql = String.format("SELECT %s FROM %s %s", 
                    property.getCreateByField(), mybatisPlusHelper.getTableInfo(ms).getTableName(), mybatisPlusHelper.where(ms, parameter));
        List<Map<String,Object>> resultList = mybatisPlusHelper.doQuery(selectCreateBySql, parameter);
        return resultList.stream().allMatch(result -> Objects.equals(userId, result.get(property.getCreateByField())));
    }

    /**
     * 是否包含create_by字段
     * 
     * @param ms
     * @return
     */
    private boolean containsCreateBy(MappedStatement ms) {
        TableInfo tableInfo = mybatisPlusHelper.getTableInfo(ms);
        return tableInfo.getFieldList().stream().anyMatch(tableFieldInfo ->
                Objects.equals(property.getCreateByField(), tableFieldInfo.getColumn()));
    }
}
