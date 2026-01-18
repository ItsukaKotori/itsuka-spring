package org.itsuka.mybatis.plus.util;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.itsuka.web.dto.PageParam;
import org.itsuka.web.dto.PageResult;

/**
 * 分页工具类
 *
 * @author Itsuka
 * @since 2025/10/25
 */
public class PageUtil {
    
    /**
     * 将PageParam转换为MyBatis Plus的Page对象
     *
     * @param pageParam 分页参数
     * @param <T>       数据类型
     * @return MyBatis Plus的Page对象
     */
    public static <T> Page<T> toPage(PageParam pageParam) {
        return new Page<>(pageParam.getCurrent(), pageParam.getSize());
    }
    
    /**
     * 将PageParam转换为MyBatis Plus的Page对象（带排序）
     *
     * @param pageParam 分页参数
     * @param orderCol  排序字段
     * @param isAsc     是否升序
     * @param <T>       数据类型
     * @return MyBatis Plus的Page对象
     */
    public static <T> Page<T> toPage(PageParam pageParam, String orderCol, boolean isAsc) {
        Page<T> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        if (orderCol != null && !orderCol.trim().isEmpty()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setColumn(orderCol);
            orderItem.setAsc(isAsc);
            page.addOrder(orderItem);
        }
        return page;
    }
    
    /**
     * 将MyBatis Plus的Page对象转换为PageResult
     *
     * @param page MyBatis Plus的Page对象
     * @param <T>  数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> toResult(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        return result;
    }
}