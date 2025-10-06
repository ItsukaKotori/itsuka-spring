package org.itsuka.core.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.itsuka.core.exception.ServiceException;
import org.springframework.http.HttpStatus;

/**
 * 统一返回信息
 *
 * @author Itsuka
 * @since 2023/2/26
 */
@Data
@NoArgsConstructor
public class Response<T> {
    /**
     * 网络状态
     */
    private HttpStatus httpStatus;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private long timestamp;

    /**
     * 请求ID（用于链路追踪）
     */
    private String requestId;

    public Response(HttpStatus status, String message, T data, String requestId) {
        this.httpStatus = status;
        this.code = status.value();
        this.success = status == HttpStatus.OK;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.requestId = requestId;
    }

    public Response(HttpStatus httpStatus, String message, T data) {
        this(httpStatus, message, data, null);
    }

    /**
     * 成功响应
     */
    public static <T> Response<T> success() {
        return new Response<>(HttpStatus.OK, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(HttpStatus.OK, "操作成功", data);
    }

    /**
     * 成功响应（带数据和消息）
     */
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(HttpStatus.OK, message, data);
    }

    /**
     * 成功响应（带请求ID）
     */
    public static <T> Response<T> success(T data, String requestId) {
        return new Response<>(HttpStatus.OK, "操作成功", data, requestId);
    }

    /**
     * 失败响应
     */
    public static Response<?> error(String message) {
        return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
    }

    /**
     * 失败响应（带错误码）
     */
    public static Response<?> error(HttpStatus status, String message) {
        return new Response<>(status, message, null);
    }

    /**
     * 失败响应（带请求ID）
     */
    public static Response<?> error(HttpStatus status, String message, String requestId) {
        return new Response<>(status, message, null, requestId);
    }

    /**
     * 服务异常响应
     */
    public static Response<?> error(ServiceException serviceException) {
        return error(serviceException.getStatus(), serviceException.getMessage());
    }

    /**
     * 服务异常响应（带请求ID）
     */
    public static Response<?> error(ServiceException serviceException, String requestId) {
        return error(serviceException.getStatus(), serviceException.getMessage(), requestId);
    }

    /**
     * 判断响应是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * 获取数据，如果失败则返回null
     */
    public T getDataOrNull() {
        return isSuccess() ? data : null;
    }

    /**
     * 获取数据，如果失败则返回默认值
     */
    public T getDataOrDefault(T defaultValue) {
        return isSuccess() ? data : defaultValue;
    }

    /**
     * 链式设置请求ID
     */
    public Response<T> withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * 链式设置时间戳
     */
    public Response<T> withTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
