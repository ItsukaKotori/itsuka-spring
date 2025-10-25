package org.itsuka.mybatis.plus.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页结果
 *
 * @author Itsuka
 * @since 2025/10/25
 */
@Data
public class PageResult<T> {
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 当前页码
     */
    private Long current;
    
    /**
     * 每页大小
     */
    private Long size;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Long pages;
    
    public PageResult() {
    }
    
    public PageResult(List<T> records, Long current, Long size, Long total) {
        this.records = records;
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = size != null && size > 0 ? (total + size - 1) / size : 0L;
    }
    
    /**
     * 从MyBatis Plus的Page对象转换
     *
     * @param page MyBatis Plus的Page对象
     * @param <T>  数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> from(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        return result;
    }
    
    /**
     * 创建空的分页结果
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 空的PageResult
     */
    public static <T> PageResult<T> empty(Long current, Long size) {
        return new PageResult<>(List.of(), current, size, 0L);
    }
}