package org.itsuka.core.domain;

import lombok.Data;
import org.itsuka.core.exception.ServiceException;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.itsuka.core.constant.Constants.ERROR_CODE;
import static org.itsuka.core.constant.Constants.SUCCESS_CODE;

/**
 * 统一返回信息
 *
 * @author Itsuka
 * @since 2023/2/26
 */
@Data
public class Response<T> {
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
    private LocalDateTime timestamp;

    /**
     * 请求ID（用于链路追踪）
     */
    private String requestId;

    private Response() {
        this.timestamp = LocalDateTime.now();
    }

    private Response(Integer code, boolean success, String message, T data) {
        this.code = code;
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    private Response(Integer code, boolean success, String message, T data, String requestId) {
        this.code = code;
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        this.requestId = requestId;
    }

    /**
     * 成功响应
     */
    public static <T> Response<T> success() {
        return new Response<>(SUCCESS_CODE, true, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(SUCCESS_CODE, true, "操作成功", data);
    }

    /**
     * 成功响应（带数据和消息）
     */
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(SUCCESS_CODE, true, message, data);
    }

    /**
     * 成功响应（带请求ID）
     */
    public static <T> Response<T> success(T data, String requestId) {
        return new Response<>(SUCCESS_CODE, true, "操作成功", data, requestId);
    }

    /**
     * 失败响应
     */
    public static Response<?> error(String message) {
        return error(ERROR_CODE, message);
    }

    /**
     * 失败响应（带错误码）
     */
    public static Response<?> error(Integer code, String message) {
        return new Response<>(code, false, message, null);
    }

    /**
     * 失败响应（带请求ID）
     */
    public static Response<?> error(Integer code, String message, String requestId) {
        return new Response<>(code, false, message, null, requestId);
    }

    /**
     * 服务异常响应
     */
    public static Response<?> error(ServiceException serviceException) {
        return error(serviceException.getCode(), serviceException.getMessage());
    }

    /**
     * 服务异常响应（带请求ID）
     */
    public static Response<?> error(ServiceException serviceException, String requestId) {
        return error(serviceException.getCode(), serviceException.getMessage(), requestId);
    }

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return success && Objects.equals(code, SUCCESS_CODE);
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
    public Response<T> withTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
