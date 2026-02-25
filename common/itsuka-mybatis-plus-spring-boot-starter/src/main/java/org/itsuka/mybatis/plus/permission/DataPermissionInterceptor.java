package org.itsuka.mybatis.plus.permission;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.itsuka.core.domain.LoginUser;
import org.itsuka.mybatis.plus.toolkit.MybatisPlusHelper;
import org.itsuka.web.utils.WebHelper;

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

    @Override
    public boolean willDoUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        if (!property.isEnable()) {
            return true;
        }
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
                    property.getCreateByField(), MybatisPlusHelper.getTableInfo(ms).getTableName(), MybatisPlusHelper.where(ms, parameter));
        List<Map<String,Object>> resultList = MybatisPlusHelper.doQuery(selectCreateBySql, parameter);
        return resultList.stream().allMatch(result -> Objects.equals(userId, result.get(property.getCreateByField())));
    }

    /**
     * 是否包含create_by字段
     * 
     * @param ms
     * @return
     */
    private boolean containsCreateBy(MappedStatement ms) {
        TableInfo tableInfo = MybatisPlusHelper.getTableInfo(ms);
        return tableInfo.getFieldList().stream().anyMatch(tableFieldInfo ->
                Objects.equals(property.getCreateByField(), tableFieldInfo.getColumn()));
    }
}
