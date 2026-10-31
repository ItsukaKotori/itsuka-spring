package org.itsuka.log.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查询条件。
 * <p>
 * 所有过滤条件均为可选。
 *
 * @author Itsuka
 */
@Data
public class OperationLogQuery {

    /**
     * 业务类型
     */
    private String type;

    /**
     * 业务子类型
     */
    private String category;

    /**
     * 业务对象标识
     */
    private String bizNo;

    /**
     * 操作人 ID
     */
    private String operatorId;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 当前页码，从 1 开始
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;
}
