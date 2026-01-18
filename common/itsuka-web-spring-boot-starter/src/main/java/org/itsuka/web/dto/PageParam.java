package org.itsuka.web.dto;

import lombok.Data;

/**
 * 分页参数
 *
 * @author Itsuka
 * @since 2025/10/25
 */
@Data
public class PageParam {
    
    /**
     * 当前页码，从1开始
     */
    private Long current = 1L;
    
    /**
     * 每页大小
     */
    private Long size = 10L;
    
    public PageParam() {
    }
    
    public PageParam(Long current, Long size) {
        this.current = current != null && current > 0 ? current : 1L;
        this.size = size != null && size > 0 ? size : 10L;
    }
}