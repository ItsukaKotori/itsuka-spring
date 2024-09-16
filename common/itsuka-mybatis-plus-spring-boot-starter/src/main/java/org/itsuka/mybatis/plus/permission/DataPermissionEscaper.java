package org.itsuka.mybatis.plus.permission;

import java.util.function.Supplier;

/**
 * 数据权限拦截器逃离类
 *
 * @author Itsuka
 * @since 2023/6/10
 */
public class DataPermissionEscaper {
    /**
     * 用于存储本次执行SQL是否要逃离权限控制的标志位
     */
    private final ThreadLocal<Boolean> ESCAPE_THREAD_LOCAL = new ThreadLocal<Boolean>();

    /**
     * 逃离权限控制 执行SQL
     * 
     * @param <T>
     * @param supplier
     * @return
     */
    public <T> T executeWithOutPermission(Supplier<T> supplier) {
        ESCAPE_THREAD_LOCAL.set(true);
        T result = supplier.get();
        // 重置标志位
        ESCAPE_THREAD_LOCAL.set(false);
        return result;
    }

    /**
     * 是否逃离权限控制
     * 
     * @return
     */
    public boolean isEscape() {
        return ESCAPE_THREAD_LOCAL.get();
    }
}