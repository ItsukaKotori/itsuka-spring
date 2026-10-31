package org.itsuka.log.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 操作日志分页结果。
 *
 * @author Itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogPage {

    /**
     * 当前页数据
     */
    private List<OperationLog> records;

    /**
     * 当前页码，从 1 开始
     */
    private long current;

    /**
     * 每页大小
     */
    private long size;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 创建分页结果，并自动计算总页数。
     *
     * @param records 当前页数据
     * @param current 当前页码
     * @param size    每页大小
     * @param total   总记录数
     * @return 分页结果
     */
    public static OperationLogPage of(List<OperationLog> records, long current, long size, long total) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new OperationLogPage(records, current, size, total, pages);
    }
}
