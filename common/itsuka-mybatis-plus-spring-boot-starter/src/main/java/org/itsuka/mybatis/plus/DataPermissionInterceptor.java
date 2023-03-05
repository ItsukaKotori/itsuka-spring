package org.itsuka.mybatis.plus;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.SQLException;

/**
 * 数据权限拦截器
 *
 * @author Itsuka
 * @date 2023/3/5
 */
public class DataPermissionInterceptor implements InnerInterceptor {

    @Override
    public boolean willDoUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        return InnerInterceptor.super.willDoUpdate(executor, ms, parameter);
    }
}
