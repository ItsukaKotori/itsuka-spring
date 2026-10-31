package org.itsuka.log.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体。
 *
 * @author Itsuka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    /**
     * 主键
     */
    private Long id;

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
     * 是否成功
     */
    private boolean success;

    /**
     * 操作内容
     */
    private String content;

    /**
     * 扩展详情
     */
    private String detail;

    /**
     * 操作人 ID
     */
    private String operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 执行方法，格式为 类名#方法名
     */
    private String method;

    /**
     * 请求 IP
     */
    private String requestIp;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 请求参数快照
     */
    private String requestParams;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
