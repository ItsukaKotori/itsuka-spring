package org.itsuka.core.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorResponse<T> extends BaseResponse<T> {
    /**
     * 创建错误响应
     *
     * @param httpStatus HTTP状态码
     * @param message    错误消息
     * @param path       请求路径
     * @return ErrorResponse实例
     */
    public static <T> ErrorResponse<T> of(HttpStatus httpStatus, String message, String path) {
        ErrorResponse<T> response = new ErrorResponse<>();
        response.setCode(httpStatus.value());
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        response.setPath(path);
        response.setHttpStatus(httpStatus);
        return response;
    }

    @Override
    public HttpStatus getHttpStatus() {
        HttpStatus httpStatus = super.getHttpStatus();
        return httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 获取响应头
     *
     * @return 响应头Map
     */
    @Override
    public Map<String, String> getResponseHeaders() {
        return new HashMap<>();
    }

    // ==================== 便捷静态方法 ====================

    /**
     * 创建并写入未授权响应（401）
     *
     * @param path       请求路径
     * @param message      错误消息
     * @return Mono<Void>
     */
    public static <T> ErrorResponse<T> unauthorized(String path, String message) {
        return of(HttpStatus.UNAUTHORIZED, message, path);
    }

    /**
     * 创建并写入禁止访问响应（403）
     *
     * @param path       请求路径
     * @param message      错误消息
     * @return Mono<Void>
     */
    public static <T> ErrorResponse<T> forbidden(String path, String message) {
        return of(HttpStatus.FORBIDDEN, message, path);
    }

    /**
     * 创建并写入内部服务器错误响应（500）
     *
     * @param path       请求路径
     * @param message      错误消息
     * @return Mono<Void>
     */
    public static <T> ErrorResponse<T> internalServerError(String path, String message) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, message, path);
    }

    /**
     * 创建并写入自定义错误响应
     *
     * @param path       请求路径
     * @param httpStatus   HTTP状态码
     * @param message      错误消息
     * @return Mono<Void>
     */
    public static <T> ErrorResponse<T> error(String path, HttpStatus httpStatus, String message) {
        return of(httpStatus, message, path);
    }
}
